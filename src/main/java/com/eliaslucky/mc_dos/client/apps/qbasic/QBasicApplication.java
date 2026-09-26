package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.blocks.computer.basic.QBasicInterpreter;
import com.eliaslucky.mc_dos.blocks.computer.basic.RunState;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import com.eliaslucky.mc_dos.client.apps.display.Screen0Text;
import com.eliaslucky.mc_dos.client.apps.editor.AbstractEditorApplication;
import com.eliaslucky.mc_dos.client.apps.editor.DialogState;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class QBasicApplication extends AbstractEditorApplication {
    public enum Mode  { EDITOR, MENU, DIALOG, RUN_OUTPUT,RUNNING }
    public enum Focus { EDIT, IMMEDIATE }

    private Mode  mode  = Mode.EDITOR;
    private Focus focus = Focus.EDIT;
    private RunState runState = RunState.FINISHED;
    private final StringBuilder inputBuffer = new StringBuilder();

    // Regions
    private final MenuBar       menuBar   = new MenuBar(QBasicMenus.ROOT);
    private final ImmediatePane immediate = new ImmediatePane();

    // Interpreter (built on each F5 run)
    private QBasicInterpreter interpreter;
    private QBasicHostImpl    host;

    private boolean altHeld = false;
    private boolean consumingMenuKeystroke = false;
    private boolean pendingExit = false;

    // Snapshot of the source so we can restore the editor after the run.
    private String pendingSourceSnapshot;

    public QBasicApplication(ComputerTerminalScreen screen, String[] args, String initialContent) {
        super(screen,
              args.length > 0 && !args[0].isEmpty() ? args[0] : "Untitled",
              initialContent);

        if (initialContent == null || initialContent.isEmpty()) {
            this.dialog = DialogState.welcome().onClosed(() -> this.dialog = null);
            this.mode   = Mode.DIALOG;
        }
    }

    private static int letterFromKey(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return key;
        return -1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    	if (pendingExit) {
            pendingExit = false;
            screen.returnToShell();
            return;
        }
    	if (mode == Mode.RUNNING) {
    	    if (runState != RunState.WAITING_INPUT) {
    	        runState = interpreter.tick(QBasicInterpreter.STEPS_PER_TICK);
    	    }
    	    if (runState == RunState.FINISHED) {
    	        mode = Mode.RUN_OUTPUT;
    	        // fall through to render the output normally
    	    }
    	    // Otherwise render the display below.
    	}
    	if (mode == Mode.RUNNING || mode == Mode.RUN_OUTPUT) {
    	    g.fill(0, 0, appWidth, appHeight, DosPalette.BLACK);
    	    displayMode.render(g, 0, 0, appWidth, appHeight);
    	    renderFooter(g);
    	    return;
    	}

        super.render(g, mouseX, mouseY, partialTick);
        if (mode == Mode.MENU) menuBar.render(g, this, appWidth);
    }
    
    @Override
    protected void renderImmediateContent(GuiGraphics g) {
        immediate.render(g, this, 0, immediateRow(), cols(),focus == Focus.IMMEDIATE);
    }
    
    @Override
    protected void renderHeader(GuiGraphics g) {
        int w = cols();
        String title = " " + filePath + " ";
        int pad = (w - title.length()) / 2;
        StringBuilder left  = new StringBuilder();
        StringBuilder right = new StringBuilder();
        for (int i = 0; i < pad; i++) left.append('\u2500');           // ─
        for (int i = 0; i < w - pad - title.length(); i++) right.append('\u2500');

        int y = CELL_H;                                                // row 1
        drawDos(g, left.toString(),  0, y, DosPalette.LIGHT_GRAY);
        drawDos(g, title,            pad * CELL_W, y, DosPalette.WHITE);
        drawDos(g, right.toString(), (pad + title.length()) * CELL_W, y, DosPalette.LIGHT_GRAY);
    }

    @Override
    protected void renderMenuBar(GuiGraphics g) {
        g.fill(0, 0, appWidth, CELL_H, DosPalette.LIGHT_GRAY);
        drawDos(g, " File   Edit   View   Search   Run   Debug   Options   Help ",
                0, 0, DosPalette.BLACK);
    }

    @Override
    protected String footerHints() {
        return switch (mode) {
            case MENU       -> menuBar.currentFooterHelp();
            case DIALOG     -> " F1=Help  Enter=Execute  Esc=Cancel  Tab=Next Field  Arrow=Next Item ";
            case RUN_OUTPUT -> " Press any key to continue ";
            case EDITOR     -> (focus == Focus.IMMEDIATE)
                    ? " Enter=Execute  F6=Editor  Esc=Cancel "
                    : " F1=Help  F2=Save  F5=Run  F6=Window  F10=Menu ";
            case RUNNING -> switch (runState) {
	            case WAITING_INPUT -> " Type your input, ENTER to submit, ESC to abort ";
	            case WAITING_SLEEP -> " Sleeping...  ESC to abort ";
	            default            -> " Running...  ESC to abort ";
	        };
        };
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean isAltKey = (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT);
        boolean altMod   = (mods & GLFW.GLFW_MOD_ALT) != 0;
        altHeld = isAltKey || altMod;
        consumingMenuKeystroke = false;

        switch (mode) {
	        case RUNNING:
	            // Abort at any time.
	            if (key == GLFW.GLFW_KEY_ESCAPE) {
	                interpreter.stop();
	                runState = RunState.FINISHED;
	                mode = Mode.RUN_OUTPUT;
	                return true;
	            }
	
	            if (runState == RunState.WAITING_INPUT) {
	                if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
	                    host.print("\n");
	                    interpreter.provideInput(inputBuffer.toString());
	                    inputBuffer.setLength(0);
	                    runState = RunState.RUNNING;
	                    return true;
	                }
	                if (key == GLFW.GLFW_KEY_BACKSPACE && inputBuffer.length() > 0) {
	                    inputBuffer.deleteCharAt(inputBuffer.length() - 1);
	                    host.backspaceChar();
	                    return true;
	                }
	            }
	            if (host != null) {
	                String arrow = arrowKeyString(key);
	                if (arrow != null) {
	                    host.enqueueKey(arrow);
	                }
	            }
	            return true;
            case RUN_OUTPUT:
                mode = Mode.EDITOR;
                restoreEditorAfterRun();
                return true;

            case MENU: {
                String action = menuBar.handleKey(key);
                if (action == null) {
                	return true;
                }
                if (action.equals("__close__")) {
                    mode = Mode.EDITOR;
                    return true;
                }
            	if (isPrintableKey(key)) consumingMenuKeystroke = true;
                invokeMenuAction(action);
                return true;
            }

            case DIALOG:
                return super.keyPressed(key, scan, mods);

            case EDITOR:
                if ((mods & GLFW.GLFW_MOD_ALT) != 0) {
                    int letter = letterFromKey(key);
                    if (letter >= 0) {
                        menuBar.openByMnemonic((char) letter);
                        if (menuBar.isActive()) { mode = Mode.MENU; return true; }
                    }
                    if (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT) {
                        menuBar.open();
                        mode = Mode.MENU;
                        return true;
                    }
                    return false;
                }
                if (key == GLFW.GLFW_KEY_F10) {
                    menuBar.open();
                    mode = Mode.MENU;
                    return true;
                }
                if (key == GLFW.GLFW_KEY_TAB) {
                	if (focus == Focus.EDIT) {
                        insertTab();
                        return true;
                    }
                    // in immediate: tab is a no-op for now (or insert too)
                    immediate.insertTab();
                    return true;
                }
                if (focus == Focus.IMMEDIATE) {
                    return handleImmediateKey(key);
                }
                return super.keyPressed(key, scan, mods);
        }
        return false;
    }
    /**
     * Maps a GLFW arrow key to the two-character sequence QBasic's
     * {@code INKEY$} returns for that key: {@code CHR$(0)} followed by a
     * letter. Programs that read arrow keys test for exactly this shape.
     *
     * @param key a {@code GLFW_KEY_*} constant
     * @return the QBasic keycode, or {@code null} if the key isn't an arrow
     */
    private static String arrowKeyString(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_UP    -> "\u0000H";
            case GLFW.GLFW_KEY_DOWN  -> "\u0000P";
            case GLFW.GLFW_KEY_LEFT  -> "\u0000K";
            case GLFW.GLFW_KEY_RIGHT -> "\u0000M";
            default -> null;
        };
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT) {
            altHeld = false;
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    protected boolean handleFunctionKey(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_F2: saveFile(); return true;
            case GLFW.GLFW_KEY_F5: startRun(); return true;
            case GLFW.GLFW_KEY_F6: focus = (focus == Focus.EDIT) ? Focus.IMMEDIATE : Focus.EDIT; return true;
        }
        return false;
    }

    // Immediate pane
    private boolean handleImmediateKey(int key) {
        switch (key) {
        	case GLFW.GLFW_KEY_F6:        focus = Focus.EDIT;    return true;
            case GLFW.GLFW_KEY_BACKSPACE: immediate.backspace(); return true;
            case GLFW.GLFW_KEY_LEFT:      immediate.moveLeft();  return true;
            case GLFW.GLFW_KEY_RIGHT:     immediate.moveRight(); return true;
            case GLFW.GLFW_KEY_UP:        immediate.moveUp();    return true;
            case GLFW.GLFW_KEY_DOWN:      immediate.moveDown();  return true;
            case GLFW.GLFW_KEY_TAB:       immediate.insertTab(); return true;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                executeImmediateLine(immediate.consume());
                return true;
        }
        return false;
    }
    
    private static boolean isPrintableKey(int key) {
        return (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z)
            || (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9)
            || key == GLFW.GLFW_KEY_SPACE
            || key == GLFW.GLFW_KEY_MINUS
            || key == GLFW.GLFW_KEY_EQUAL
            || key == GLFW.GLFW_KEY_LEFT_BRACKET
            || key == GLFW.GLFW_KEY_RIGHT_BRACKET
            || key == GLFW.GLFW_KEY_SEMICOLON
            || key == GLFW.GLFW_KEY_APOSTROPHE
            || key == GLFW.GLFW_KEY_GRAVE_ACCENT
            || key == GLFW.GLFW_KEY_BACKSLASH
            || key == GLFW.GLFW_KEY_COMMA
            || key == GLFW.GLFW_KEY_PERIOD
            || key == GLFW.GLFW_KEY_SLASH;
    }

    private void executeImmediateLine(String line) {
        if (line == null || line.isBlank()) return;

        // Collect output as text - do NOT hijack the main display.
        HeadlessHost headless = new HeadlessHost();
        new QBasicInterpreter(headless).run(line);
        if (headless.hadError()) {
            showSyntaxError(headless.getLastErrorCode(), headless.getLastErrorMessage());
            // Do NOT clear the buffer - the user can fix and re-run.
            return;
        }

        immediate.appendOutput(headless.getOutput());
    }
    
    private void showSyntaxError(int errCode, String message) {
        dialog = new DialogState()
                .addLine("")
                .addLine(message == null ? "Invalid syntax" : message)
                .addLine("")
                .addItem("OK",   "err.ok")
                .addItem("Help", "err.help")
                .onClosed(() -> { dialog = null; mode = Mode.EDITOR; });
        pendingErrorCode = errCode;
        mode = Mode.DIALOG;
    }

    private int pendingErrorCode = 2;

    private void invokeMenuAction(String action) {
        mode = Mode.EDITOR;
        switch (action) {
            case "file.exit":
            	pendingExit = true;
                //screen.returnToShell();
                return;
            case "file.save":
                saveFile();
                return;
            case "run.start":
                startRun();
                return;
            case "help.survival":
                dialog = new DialogState()
                        .addLine("")
                        .addLine("QBasic Survival Guide")
                        .addLine("")
                        .addLine("F5 runs your program.")
                        .addLine("F2 saves to disk.")
                        .addLine("ALT opens the menu bar.")
                        .addLine("")
                        .addItem("Press ESC to close", "close")
                        .onClosed(() -> { dialog = null; mode = Mode.EDITOR; });
                mode = Mode.DIALOG;
                return;
            case "err.ok":
                dialog = null;
                mode = Mode.EDITOR;
                return;

            case "err.help":
                dialog = new DialogState()
                        .addLine("")
                        .addLine("ERR code: " + pendingErrorCode)
                        .addLine("")
                        .addLine("Press ENTER to continue")
                        .addLine("")
                        .addItem("OK", "err.ok")
                        .onClosed(() -> { dialog = null; mode = Mode.EDITOR; });
                mode = Mode.DIALOG;
                return;
            default:
                statusMessage = "(action: " + action + ")";
        }
    }

    @Override
    public boolean charTyped(char cp, int mods) {
    	if (mode == Mode.RUNNING && runState != RunState.WAITING_INPUT && host != null) {
            if (cp >= 32 && cp != 127) {
                host.enqueueKey(String.valueOf(cp));
            }
            return true;
        }

        if (mode == Mode.RUNNING && runState == RunState.WAITING_INPUT) {
            if (cp >= 32 && cp != 127) {
                inputBuffer.append(cp);
                host.print(String.valueOf(cp));
            }
            return true;
        }
    	if (mode == Mode.RUNNING && host != null) {
            host.enqueueKey(String.valueOf(cp));
            return true;
        }
    	if (consumingMenuKeystroke) {
            consumingMenuKeystroke = false;
            return true;
        }
        if (mode == Mode.MENU)       return true;
        if (mode == Mode.DIALOG)     return true;
        if (mode == Mode.RUN_OUTPUT) return true;

        if (mode == Mode.EDITOR && focus == Focus.IMMEDIATE) {
            immediate.insert(cp);
            return true;
        }
        if (altHeld) return true;
        return super.charTyped(cp, mods);
    }
    
    @Override
    protected boolean shouldDrawCursor() {
        return mode == Mode.EDITOR && focus == Focus.EDIT;
    }
    
    @Override
    protected boolean modeIsEditor() {
        return mode == Mode.EDITOR;
    }

    // Run pipeline
    private void startRun() {
        pendingSourceSnapshot = currentSource();

        // Fresh text screen for the program's output.
        setDisplayMode(new Screen0Text());

        host = new QBasicHostImpl(this);
        interpreter = new QBasicInterpreter(host);
        interpreter.start(pendingSourceSnapshot);

        inputBuffer.setLength(0);
        runState = RunState.RUNNING;
        mode = Mode.RUNNING;
    }

    private void restoreEditorAfterRun() {
        if (pendingSourceSnapshot == null) return;
        lines.clear();
        for (String l : pendingSourceSnapshot.split("\n", -1)) lines.add(new StringBuilder(l));
        cursorRow = 0; cursorCol = 0;
        scrollRow = 0; scrollCol = 0;
        pendingSourceSnapshot = null;

        // Restore to a fresh text screen so a subsequent run starts clean.
        setDisplayMode(new Screen0Text());
    }

    @Override
    public String getTitle() { return "QBASIC - " + filePath; }
}