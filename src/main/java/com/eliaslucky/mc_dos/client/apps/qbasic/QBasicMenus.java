package com.eliaslucky.mc_dos.client.apps.qbasic;

import java.util.List;

import com.eliaslucky.mc_dos.client.tui.TuiMenu.Menu;
import com.eliaslucky.mc_dos.client.tui.TuiMenu.Item;

/*
 * Top menus
 * */
public final class QBasicMenus {
    private QBasicMenus() {}

    public static final List<Menu> ROOT = List.of(
        new Menu("File", 'F', List.of(
            new Item("New Program",     'N', "file.new",    "Clears the current program"),
            new Item("Open Program...", 'O', "file.open",   "Loads a program from disk"),
            new Item("Save",            'S', "file.save",   "Saves the current program"),
            new Item("Save As...",      'A', "file.saveas", "Saves under a new name"),
            new Item("Print...",        'P', "file.print",  "Prints the current file"),
            new Item("Exit",            'X', "file.exit",   "Exits QBASIC and returns to DOS")
        )),
        new Menu("Edit", 'E', List.of(
            new Item("Cut",   'C', "edit.cut",   "Removes the selection and copies it to the clipboard"),
            new Item("Copy",  'O', "edit.copy",  "Copies the selection to the clipboard"),
            new Item("Paste", 'P', "edit.paste", "Inserts a copy of the clipboard contents"),
            new Item("Clear", 'L', "edit.clear", "Deletes the selection")
        )),
        new Menu("View", 'V', List.of(
            new Item("SUBs...",       'S', "view.subs",   "Shows the list of SUBs in this program"),
            new Item("Split",         'P', "view.split",  "Toggles the split window"),
            new Item("Output Screen", 'O', "view.output", "Displays the output screen")
        )),
        new Menu("Search", 'S', List.of(
            new Item("Find...",          'F', "search.find",   "Searches for text"),
            new Item("Repeat Last Find", 'R', "search.repeat", "Repeats the last Find command"),
            new Item("Change...",        'C', "search.change", "Finds and replaces text")
        )),
        new Menu("Run", 'R', List.of(
            new Item("Start",    'S', "run.start",    "Runs the current program"),
            new Item("Restart",  'R', "run.restart",  "Resets variables and runs the program"),
            new Item("Continue", 'C', "run.continue", "Resumes execution after a pause")
        )),
        new Menu("Debug", 'D', List.of(
            new Item("Step",       'S', "debug.step",    "Executes one line at a time"),
            new Item("Trace On",   'T', "debug.traceon", "Displays each statement as it runs"),
            new Item("Breakpoint", 'B', "debug.break",   "Sets a breakpoint")
        )),
        new Menu("Options", 'O', List.of(
            new Item("Display...",   'D', "opts.display",  "Changes display settings"),
            new Item("Help Path...", 'H', "opts.helppath", "Sets the path for Help files")
        )),
        new Menu("Help", 'H', List.of(
            new Item("Index",    'I', "help.index",    "Displays the Help Index"),
            new Item("Contents", 'C', "help.contents", "Displays the Table of Contents"),
            new Item("About...", 'A', "help.about",    "Displays information about QBASIC")
        ))
    );

    public static int indexOfMnemonic(char m) {
        for (int i = 0; i < ROOT.size(); i++) {
            if (Character.toUpperCase(ROOT.get(i).mnemonic()) == Character.toUpperCase(m)) return i;
        }
        return -1;
    }
}
