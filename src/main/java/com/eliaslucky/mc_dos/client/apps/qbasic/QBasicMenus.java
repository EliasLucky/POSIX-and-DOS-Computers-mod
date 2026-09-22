public record MenuItem(String label, int hotkey, String action, String footerHelp) {}
public record Menu(String label, char mnemonic, List<MenuItem> items) {}

public final class QBasicMenus {
    public static final List<Menu> ROOT = List.of(
        new Menu("File", 'F', List.of(
            new MenuItem("New Program",      'N', "file.new",      "Clears the current program"),
            new MenuItem("Open Program...",  'O', "file.open",     "Loads a program from disk"),
            new MenuItem("Save",             'S', "file.save",     "Saves the current program"),
            new MenuItem("Save As...",       'A', "file.saveas",   "Saves under a new name"),
            new MenuItem("Print...",         'P', "file.print",    "Prints the current file"),
            new MenuItem("Exit",             'X', "file.exit",     "Exits QBASIC and returns to DOS")
        )),
        new Menu("Edit", 'E', List.of(/* Cut Copy Paste Clear ... */)),
        new Menu("View", 'V', List.of(/* Output Screen, ... */)),
        new Menu("Search", 'S', List.of(/* Find, Repeat Last Find, Change */)),
        new Menu("Run", 'R', List.of(
            new MenuItem("Start",            'S', "run.start",    "Runs the current program"),
            new MenuItem("Restart",          'R', "run.restart",  "Resets variables and runs"),
            new MenuItem("Continue",         'C', "run.continue", "Resumes execution after a pause")
        )),
        new Menu("Debug", 'D', List.of(/* ... */)),
        new Menu("Options", 'O', List.of(/* Display, Help Path */)),
        new Menu("Help",  'H', List.of(/* Index, Contents, About... */))
    );
}
