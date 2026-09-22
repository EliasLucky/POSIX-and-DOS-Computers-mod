package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.blocks.computer.basic.QBasicInterpreter;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import com.eliaslucky.mc_dos.client.apps.display.Screen0Text;
import com.eliaslucky.mc_dos.client.apps.editor.AbstractEditorApplication;
import com.eliaslucky.mc_dos.client.apps.editor.DialogState;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class QBasicApplication extends AbstractEditorApplication {
    public enum Mode  { EDITOR, MENU, DIALOG, RUN_OUTPUT }
    public enum Focus { EDIT, IMMEDIATE }

    private Mode  mode  = Mode.EDITOR;
    private Focus focus = Focus.EDIT;

    // Regions
    private final MenuBar       menuBar   = new MenuBar(QBasicMenus.ROOT);
    private final ImmediatePane immediate = new ImmediatePane();

    // Interpreter (built on each F5 run)
    private QBasicInterpreter interpreter;
    private QBasicHostImpl    host;

    private boolean altHeld = false;

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
        if (mode == Mode.RUN_OUTPUT) {
            // Full-screen takeover: the display mode IS the output surface.
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
        immediate.render(g, this, 0, (immediateY() / CELL_H) + 1, cols());
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
                mode = Mode.EDITOR;
                restoreEditorAfterRun();
                return true;

            case MENU: {
                String action = menuBar.handleKey(key);
                if (action == null || action.equals("__close__")) {
                    mode = Mode.EDITOR;
                    return true;
                }
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

    // Immediate pane
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

        // Collect output as text — do NOT hijack the main display.
        HeadlessHost headless = new HeadlessHost();
        new QBasicInterpreter(headless).run(line);
        immediate.appendOutput(headless.getOutput());
        // Stay in EDITOR mode. The result is shown inline.
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
        if (mode == Mode.MENU)       return true;
        if (mode == Mode.DIALOG)     return true;
        if (mode == Mode.RUN_OUTPUT) return true;

        if (mode == Mode.EDITOR && focus == Focus.IMMEDIATE) {
            immediate.type(cp);
            return true;
        }
        if (altHeld) return true;
        return super.charTyped(cp, mods);
    }

    // Run pipeline
    private void startRun() {
        pendingSourceSnapshot = currentSource();

        // Fresh text screen for the program's output.
        setDisplayMode(new Screen0Text());

        host = new QBasicHostImpl(this);
        interpreter = new QBasicInterpreter(host);
        interpreter.run(pendingSourceSnapshot);

        mode = Mode.RUN_OUTPUT;
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