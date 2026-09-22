package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import com.eliaslucky.mc_dos.client.apps.editor.AbstractEditorApplication;
import com.eliaslucky.mc_dos.client.apps.editor.DialogState;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class QBasicApplication extends AbstractEditorApplication {
    public enum Mode  { EDITOR, MENU, DIALOG, RUNNING, RUN_OUTPUT }
    public enum Focus { EDIT, IMMEDIATE }

    private Mode  mode  = Mode.EDITOR;
    private Focus focus = Focus.EDIT;

    // Regions
    private final MenuBar          menuBar    = new MenuBar(QBasicMenus.ROOT);
    private final ImmediatePane    immediate  = new ImmediatePane();
    private final RunOutputPane    runOutput  = new RunOutputPane();

    // Interpreter
    private QBasicInterpreter   interpreter;
    private QBasicHostImpl      host;
    private boolean             awaitingRunReturn = false;
    
    private boolean altHeld = false;

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

        if (mode == Mode.RUN_OUTPUT || mode == Mode.RUNNING) {
            // Full-screen takeover — no editor chrome.
            g.fill(0, 0, appWidth, appHeight, DosPalette.BLACK);
            displayMode.render(g, 0, 0, appWidth, appHeight);
            renderFooter(g);
            return;
        }

        // Everything else keeps the editor chrome underneath.
        super.render(g, mouseX, mouseY, partialTick);

        if (mode == Mode.MENU) {
            menuBar.render(g, this, appWidth);
        }
        // DIALOG is already rendered inside super.render() via this.dialog.
        // (AbstractEditorApplication.render draws it last, on top of everything.)
    }
    
    @Override
    protected void renderMenuBar(GuiGraphics g) {
    	g.fill(0, 0, appWidth, CELL_H, DosPalette.LIGHT_GRAY);
    	drawDos(g, " File   Edit   View   Search   Run   Debug   Options   Help ",0,0,DosPalette.BLACK);
    }

    @Override
    protected String footerHints() {
        return switch (mode) {
            case MENU      -> menuBar.currentFooterHelp();
            case DIALOG    -> " F1=Help  Enter=Execute  Esc=Cancel  Tab=Next Field  Arrow=Next Item ";
            case RUNNING   -> " Running...  Ctrl+Break to stop ";
            case RUN_OUTPUT-> " Press any key to continue ";
            case EDITOR    -> (focus == Focus.IMMEDIATE)
                    ? " Enter=Execute  Tab=Editor  Esc=Cancel "
                    : " F1=Help  F2=Save  F5=Run  F6=Window  F10=Menu ";
        };
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean isAltKey = (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT);
        boolean altMod   = (mods & GLFW.GLFW_MOD_ALT) != 0;
        altHeld = isAltKey || altMod;

        switch (mode) {

            case RUN_OUTPUT:
                // Any key returns to the editor, restoring the code buffer.
                awaitingRunReturn = false;
                mode = Mode.EDITOR;
                restoreEditorAfterRun();
                return true;

            case RUNNING:
                if (key == GLFW.GLFW_KEY_ESCAPE) {
                    interpreter.stop();
                    host.end();
                    finishRun();
                }
                return true;

            case MENU: {
                String action = menuBar.handleKey(key);
                if (action == null) {                 // menu dismissed
                    mode = Mode.EDITOR;
                    return true;
                }
                if (action.equals("__close__")) {
                    mode = Mode.EDITOR;
                    return true;
                }
                invokeMenuAction(action);
                return true;
            }

            case DIALOG:
                return super.keyPressed(key, scan, mods); // dialog handles

            case EDITOR:
                // ALT opens the menu bar
                //if (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT) {
                //    menuBar.open();
                //    mode = Mode.MENU;
                //    return true;
                //}
		if ((mods & GLFW.GLFW_MOD_ALT) != 0) {
        int letter = letterFromKey(key);
        if (letter >= 0) {
            menuBar.openByMnemonic((char) letter);
            if (menuBar.isActive()) { mode = Mode.MENU; return true; }
        }
        // ALT alone (no letter yet) — still open the bar at the first menu.
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
                    focus = (focus == Focus.EDIT) ? Focus.IMMEDIATE : Focus.EDIT;
                    return true;
                }
                if (focus == Focus.IMMEDIATE) {
                    return handleImmediateKey(key);
                }
                return super.keyPressed(key, scan, mods);
        }
        return false;
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
            case GLFW.GLFW_KEY_F6: focus = Focus.IMMEDIATE; return true;
        }
        return false;
    }

    // Immediate pane input
    private boolean handleImmediateKey(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE: immediate.backspace(); return true;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                executeImmediateLine(immediate.consume());
                return true;
        }
        return false;
    }

    private void executeImmediateLine(String line) {
        if (line == null || line.isBlank()) return;
        // Run the line as a one-liner program.
        var host = new QBasicHostImpl(this);
        new QBasicInterpreter(host).run(line);
        for (String s : host.getOutput()) runOutput.append(s);
        // Also print into the immediate history so the user sees it inline.
        immediate.appendOutput(host.getOutput());
    }

    private void invokeMenuAction(String action) {
        mode = Mode.EDITOR;
        switch (action) {
            case "file.exit":
                screen.returnToShell();
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
            default:
                statusMessage = "(action: " + action + ")";
        }
    }

    @Override
    public boolean charTyped(char cp, int mods) {
        if (mode == Mode.MENU)               return true;
        if (mode == Mode.DIALOG)             return true;
        if (mode == Mode.RUN_OUTPUT)         return true;
        if (mode == Mode.EDITOR && focus == Focus.IMMEDIATE) {
            immediate.type(cp);
            return true;
        }

        if (altHeld) return true;

        return super.charTyped(cp, mods);
    }

    private void startRun() {
        // Snapshot current source so we can restore the editor afterwards.
        pendingSourceSnapshot = currentSource();

        runOutput.clear();
        host = new QBasicHostImpl(this);
        interpreter = new QBasicInterpreter(host);

        try {
            interpreter.run(pendingSourceSnapshot);
        } catch (Exception ex) {
            host.printNewline();
            host.getOutput().add("Error: " + ex.getMessage());
        }

        for (String line : host.getOutput()) runOutput.append(line);
        runOutput.append("");
        runOutput.append("Press any key to continue");

        mode = Mode.RUN_OUTPUT;
        awaitingRunReturn = true;
    }

    private void finishRun() {
        for (String line : host.getOutput()) runOutput.append(line);
        runOutput.append("");
        runOutput.append("Press any key to continue");
        mode = Mode.RUN_OUTPUT;
    }

    private String pendingSourceSnapshot;

    private void restoreEditorAfterRun() {
        // Put the original source back into the edit buffer.
        if (pendingSourceSnapshot == null) return;
        lines.clear();
        for (String l : pendingSourceSnapshot.split("\n", -1)) lines.add(new StringBuilder(l));
        cursorRow = 0; cursorCol = 0;
        scrollRow = 0; scrollCol = 0;
        pendingSourceSnapshot = null;
    }

    @Override
    public String getTitle() { return "QBASIC - " + filePath; }
}
