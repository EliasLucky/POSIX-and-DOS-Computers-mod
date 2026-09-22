public class QBasicApplication extends TerminalApplication {

    public enum Mode { EDITOR, MENU, DIALOG, RUNNING, RUN_OUTPUT }

    private Mode mode = Mode.EDITOR;

    // Regions
    private final EditorPane   editPane   = new EditorPane();
    private final ImmediatePane immediate = new ImmediatePane();
    private final MenuBar       menuBar    = new MenuBar(QBasicMenus.ROOT);
    private final DialogState   dialog     = new DialogState();
    private final RunOutputPane runOutput  = new RunOutputPane();

    // The interpreter is created lazily on F5.
    private QBasicInterpreter interpreter;

    // Which region has focus in EDITOR mode.
    private Focus focus = Focus.EDIT;

    public enum Focus { EDIT, IMMEDIATE }
}
