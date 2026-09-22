package com.eliaslucky.mc_dos.client.apps.qbasic;

import java.util.List;

record MenuItem(String label, char mnemonic, String action, String footerHelp) {}
record Menu(String label, char mnemonic, List<MenuItem> items) {
    /** Index of the mnemonic character inside label, or -1 if absent. */
    public int mnemonicIndex() {
        int i = label.toUpperCase().indexOf(Character.toUpperCase(mnemonic));
        return i;
    }
}

public final class QBasicMenus {
    private QBasicMenus() {}

    public static final List<Menu> ROOT = List.of(
        new Menu("File", 'F', List.of(
            new MenuItem("New Program",     'N', "file.new",    "Clears the current program"),
            new MenuItem("Open Program...", 'O', "file.open",   "Loads a program from disk"),
            new MenuItem("Save",            'S', "file.save",   "Saves the current program"),
            new MenuItem("Save As...",      'A', "file.saveas", "Saves under a new name"),
            new MenuItem("Print...",        'P', "file.print",  "Prints the current file"),
            new MenuItem("Exit",            'X', "file.exit",   "Exits QBASIC and returns to DOS")
        )),
        new Menu("Edit", 'E', List.of(
            new MenuItem("Cut",   'C', "edit.cut",   "Removes the selection and copies it to the clipboard"),
            new MenuItem("Copy",  'O', "edit.copy",  "Copies the selection to the clipboard"),
            new MenuItem("Paste", 'P', "edit.paste", "Inserts a copy of the clipboard contents"),
            new MenuItem("Clear", 'L', "edit.clear", "Deletes the selection")
        )),
        new Menu("View", 'V', List.of(
            new MenuItem("SUBs...",       'S', "view.subs",   "Shows the list of SUBs in this program"),
            new MenuItem("Split",         'P', "view.split",  "Toggles the split window"),
            new MenuItem("Output Screen", 'O', "view.output", "Displays the output screen")
        )),
        new Menu("Search", 'S', List.of(
            new MenuItem("Find...",          'F', "search.find",   "Searches for text"),
            new MenuItem("Repeat Last Find", 'R', "search.repeat", "Repeats the last Find command"),
            new MenuItem("Change...",        'C', "search.change", "Finds and replaces text")
        )),
        new Menu("Run", 'R', List.of(
            new MenuItem("Start",    'S', "run.start",    "Runs the current program"),
            new MenuItem("Restart",  'R', "run.restart",  "Resets variables and runs the program"),
            new MenuItem("Continue", 'C', "run.continue", "Resumes execution after a pause")
        )),
        new Menu("Debug", 'D', List.of(
            new MenuItem("Step",       'S', "debug.step",    "Executes one line at a time"),
            new MenuItem("Trace On",   'T', "debug.traceon", "Displays each statement as it runs"),
            new MenuItem("Breakpoint", 'B', "debug.break",   "Sets a breakpoint")
        )),
        new Menu("Options", 'O', List.of(
            new MenuItem("Display...",   'D', "opts.display",  "Changes display settings"),
            new MenuItem("Help Path...", 'H', "opts.helppath", "Sets the path for Help files")
        )),
        new Menu("Help", 'H', List.of(
            new MenuItem("Index",    'I', "help.index",    "Displays the Help Index"),
            new MenuItem("Contents", 'C', "help.contents", "Displays the Table of Contents"),
            new MenuItem("About...", 'A', "help.about",    "Displays information about QBASIC")
        ))
    );

    public static int indexOfMnemonic(char m) {
        for (int i = 0; i < ROOT.size(); i++) {
            if (Character.toUpperCase(ROOT.get(i).mnemonic()) == Character.toUpperCase(m)) return i;
        }
        return -1;
    }
}
