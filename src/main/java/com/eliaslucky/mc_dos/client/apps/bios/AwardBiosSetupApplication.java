package com.eliaslucky.mc_dos.client.apps.bios;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.tui.TuiBox;
import com.eliaslucky.mc_dos.client.tui.TuiDialog;
import com.eliaslucky.mc_dos.client.tui.TuiScreen;
import com.eliaslucky.mc_dos.network.ModMessages;
import com.eliaslucky.mc_dos.network.ServerboundSaveBiosConfigPacket;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Phoenix-Award BIOS v6.00PG SETUP screen.
 *
 * <p>Visual style matches the blue-on-blue Award BIOS that shipped on
 * Pentium-class motherboards from the late 1990s through the 2000s:
 * a yellow title bar, a tabbed menu strip, section headers with a
 * ▶ bullet, values in yellow, and a light gray "Item Help" panel on
 * the right.
 *
 * <p>Navigation follows the real BIOS: arrow keys move between items,
 * left/right switch tabs, Enter opens a picker, +/- or PgUp/PgDn
 * cycle values, F5 restores the config as it was when SETUP opened,
 * F6 loads fail-safe defaults, F7 loads optimized defaults, F10 saves
 * and exits, and Esc prompts to discard changes.
 *
 * <p>Only the fields the mod actually models are editable. Boot device
 * priority, USB controllers, power management, and the many toggles a
 * real BIOS exposed are absent because the machine has no hardware
 * behind them.
 *
 * @since 1.
 */
public class AwardBiosSetupApplication extends TerminalApplication {
    // Colors
    // Sampled from the Award BIOS v6.00PG default palette.

    /** Blue background — the dominant color of the whole screen. */
    private static final int BG         = 0xFF0000AA;

    /** Yellow text used for the title bar, active tab values, and the like. */
    private static final int TITLE_FG   = 0xFFFFFF55;

    /** Inactive top-level tabs. */
    private static final int TAB_INACT  = 0xFFFFFF55;

    /** Active tab background. */
    private static final int TAB_ACT_BG = 0xFFAAAAAA;

    /** Active tab text. */
    private static final int TAB_ACT_FG = 0xFF000000;

    /** Section headers (with ▶ prefix) — cyan-yellow blend common in Award. */
    private static final int SECTION_FG = 0xFF55FFFF;

    /** Normal item label. */
    private static final int LABEL_FG   = 0xFFFFFFFF;

    /** Item value — always yellow in Award BIOS. */
    private static final int VALUE_FG   = 0xFFFFFF55;

    /** Selected row background. */
    private static final int SEL_BG     = 0xFFAAAAAA;

    /** Selected row text. */
    private static final int SEL_FG     = 0xFF000000;

    /** Item-help box border and text. */
    private static final int HELP_FG    = 0xFFAAAAAA;

    /** Footer bar. */
    private static final int FOOT_BG    = 0xFFAAAAAA;

    /** Footer bar text. */
    private static final int FOOT_FG    = 0xFF000000;

    // Tabs

    /** Top-level tabs, matching the compressed Award v6.00PG menu bar. */
    private enum Tab {
        MAIN       ("Main"),
        ADVANCED   ("Advanced"),
        PERIPHERALS("Peripherals"),
        BOOT       ("Boot"),
        EXIT       ("Exit");

        final String label;
        Tab(String label) { this.label = label; }
    }

    // Row model

    /**
     * A single line in the body of the current tab.
     *
     * <p>{@code HEADER} rows are section titles, not selectable.
     * {@code ITEM} rows have a label and value; {@code action} may be
     * {@code null} for read-only items. {@code BLANK} rows are spacing.
     */
    private static final class Row {
        enum Kind { HEADER, ITEM, BLANK }

        final Kind   kind;
        final String label;
        final String value;
        final String action;
        final String help;

        private Row(Kind kind, String label, String value,
                    String action, String help) {
            this.kind   = kind;
            this.label  = label;
            this.value  = value;
            this.action = action;
            this.help   = help;
        }

        static Row header(String text) {
            return new Row(Kind.HEADER, text, null, null, null);
        }
        static Row item(String label, String value, String action, String help) {
            return new Row(Kind.ITEM, label, value, action, help);
        }
        static Row readOnly(String label, String value, String help) {
            return new Row(Kind.ITEM, label, value, null, help);
        }
        static Row blank() {
            return new Row(Kind.BLANK, null, null, null, null);
        }
    }

    // State

    /** The config as it was when SETUP opened  F5 restores it. */
    private final MachineConfig original;

    /** The working copy. Edits modify this. */
    private MachineConfig config;

    /** BIOS name for the title. */
    private final String biosName;

    private Tab activeTab = Tab.MAIN;
    private int selectedRow = 0;

    /** Rows of the current tab, rebuilt on every config change. */
    private final List<Row> bodyRows = new ArrayList<>();

    /** Modal dialog overlay. */
    private final TuiScreen widgets = new TuiScreen();

    public AwardBiosSetupApplication(ComputerTerminalScreen screen, MachineConfig config, String biosName) {
        super(screen);
        this.original  = config;
        this.config    = config;
        this.biosName  = biosName;
        rebuildRows();
        selectFirstSelectable();
    }

    // Row building
    private void rebuildRows() {
        bodyRows.clear();
        switch (activeTab) {
            case MAIN        -> rebuildMain();
            case ADVANCED    -> rebuildAdvanced();
            case PERIPHERALS -> rebuildPeripherals();
            case BOOT        -> rebuildBoot();
            case EXIT        -> rebuildExit();
        }
    }

    private void rebuildMain() {
        bodyRows.add(Row.header("Standard CMOS Features"));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.item("Date (mm:dd:yy)", awardDate(config.systemTime()),
                "edit.date", "Set the system date."));
        bodyRows.add(Row.item("Time (hh:mm:ss)", awardTime(config.systemTime()),
                "edit.time", "Set the system time."));
        bodyRows.add(Row.blank());

        bodyRows.add(Row.item(
                "IDE Channel 0 Master",
                config.hardDisk1() == MachineConfig.DiskType.NONE
                        ? "None" : config.hardDisk1().displayName(),
                "edit.hd1",
                "Select the type of hard disk on the primary master."));
        bodyRows.add(Row.readOnly("IDE Channel 0 Slave", "None", "Not present."));
        bodyRows.add(Row.readOnly("IDE Channel 1 Master", "None", "Not present."));
        bodyRows.add(Row.readOnly("IDE Channel 1 Slave", "None", "Not present."));
        bodyRows.add(Row.blank());

        bodyRows.add(Row.item("Drive A", config.floppyA().displayName(),
                "edit.floppyA", "Select the type of floppy drive on the A: bay."));
        bodyRows.add(Row.item("Drive B", config.floppyB().displayName(),
                "edit.floppyB", "Select the type of floppy drive on the B: bay."));
        bodyRows.add(Row.blank());

        bodyRows.add(Row.readOnly("Base Memory", config.baseMemoryKb() + "K",
                "Conventional memory detected during POST."));
        bodyRows.add(Row.readOnly("Extended Memory", config.extendedMemoryKb() + "K",
                "Memory beyond 1 MB detected during POST."));
        bodyRows.add(Row.readOnly("Total Memory",
                (config.baseMemoryKb() + config.extendedMemoryKb()) + "K",
                "Total memory installed in the system."));
    }

    private void rebuildAdvanced() {
        bodyRows.add(Row.header("Advanced BIOS Features"));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.item("Video", config.primaryDisplay().displayName(),
                "edit.display", "Select the default video device."));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.item("Math Coprocessor",
                config.mathCoprocessor() ? "Installed" : "Not Installed",
                "edit.coprocessor",
                "Enable or disable the built-in floating-point coprocessor."));
    }

    private void rebuildPeripherals() {
        boolean hasFloppy =
                config.floppyA() != MachineConfig.FloppyType.NONE
             || config.floppyB() != MachineConfig.FloppyType.NONE;

        bodyRows.add(Row.header("Integrated Peripherals"));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.readOnly("On-Chip Primary PCI IDE", "Enabled",
                "The primary IDE controller is always enabled."));
        bodyRows.add(Row.readOnly("On-Chip Secondary PCI IDE", "Enabled",
                "The secondary IDE controller is always enabled."));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.readOnly("Onboard FDC Controller",
                hasFloppy ? "Enabled" : "Disabled",
                "Floppy disk controller; enabled when a floppy drive is present."));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.readOnly("Onboard Serial Port 1", "3F8/IRQ4",
                "The first serial port uses the standard I/O address."));
        bodyRows.add(Row.readOnly("Onboard Serial Port 2", "2F8/IRQ3",
                "The second serial port uses the standard I/O address."));
        bodyRows.add(Row.readOnly("Onboard Parallel Port", "378/IRQ7",
                "The parallel port uses the standard I/O address."));
    }

    private void rebuildBoot() {
        bodyRows.add(Row.header("Boot Settings"));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.readOnly("Boot Device Priority",
                config.hardDisk1() == MachineConfig.DiskType.NONE
                        ? "No bootable device"
                        : "C: (Hard Disk)",
                "This emulated BIOS boots from the primary hard disk."));
        bodyRows.add(Row.readOnly("Removable Boot",
                config.floppyA() != MachineConfig.FloppyType.NONE
                        ? "Enabled" : "Disabled",
                "Whether the BIOS may boot from a floppy."));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.readOnly("Quick Power On Self Test", "Enabled",
                "Skip the extended memory test for a faster boot."));
    }

    private void rebuildExit() {
        bodyRows.add(Row.header("Save & Exit Setup"));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.item("Save & Exit Setup", "<Enter>",
                "action.save",
                "Save changes to CMOS and exit SETUP."));
        bodyRows.add(Row.item("Exit Without Saving", "<Enter>",
                "action.exit",
                "Discard changes and exit SETUP."));
        bodyRows.add(Row.blank());
        bodyRows.add(Row.item("Load Optimized Defaults", "<Enter>",
                "action.defaults.optimized",
                "Load the optimized default configuration."));
        bodyRows.add(Row.item("Load Fail-Safe Defaults", "<Enter>",
                "action.defaults.failsafe",
                "Load the fail-safe default configuration."));
    }

    private void selectFirstSelectable() {
        for (int i = 0; i < bodyRows.size(); i++) {
            if (bodyRows.get(i).kind == Row.Kind.ITEM) {
                selectedRow = i;
                return;
            }
        }
        selectedRow = 0;
    }

    // Rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, appWidth, appHeight, BG);

        drawTitleBar(g);
        drawMenuBar(g);
        drawBody(g);
        drawItemHelp(g);
        drawFooter(g);

        widgets.render(g, this);
    }

    private void drawTitleBar(GuiGraphics g) {
        String title =
                "CMOS Setup Utility - Copyright (C) 1984-2003 Award Software, Inc.";
        drawDos(g, title, 2 * CELL_W, 0, TITLE_FG);
    }

    private void drawMenuBar(GuiGraphics g) {
        int col = 2;
        for (Tab tab : Tab.values()) {
            boolean active = (tab == activeTab);
            int labelLen = tab.label.length();
            int px = col * CELL_W;
            int py = CELL_H;

            if (active) {
                g.fill(px, py,
                       px + labelLen * CELL_W, py + CELL_H,
                       TAB_ACT_BG);
                drawDos(g, tab.label, px, py, TAB_ACT_FG);
            } else {
                drawDos(g, tab.label, px, py, TAB_INACT);
            }
            col += labelLen + 2;
        }
    }

    private void drawBody(GuiGraphics g) {
        int topRow = 3;
        int visible = Math.max(0, rows() - topRow - 2);

        for (int i = 0; i < bodyRows.size() && i < visible; i++) {
            Row row = bodyRows.get(i);
            int py = (topRow + i) * CELL_H;

            switch (row.kind) {
                case HEADER -> drawDos(g,
                        "\u25B6 " + row.label,
                        2 * CELL_W, py, SECTION_FG);

                case ITEM -> {
                    boolean selected = (i == selectedRow);
                    int px = 2 * CELL_W;
                    int pw = 76 * CELL_W;

                    if (selected) {
                        g.fill(px, py, px + pw, py + CELL_H, SEL_BG);
                    }

                    int labelColor = selected ? SEL_FG : LABEL_FG;
                    int valueColor = selected ? SEL_FG : VALUE_FG;

                    drawDos(g, row.label, 2 * CELL_W, py, labelColor);
                    if (row.value != null) {
                        drawDos(g, row.value, 24 * CELL_W, py, valueColor);
                    }
                }

                case BLANK -> { /* spacing */ }
            }
        }
    }

    private void drawItemHelp(GuiGraphics g) {
        int startCol = 44;
        int endCol   = 78;
        int startRow = 3;
        int endRow   = rows() - 3;
        if (endRow <= startRow) return;

        // Framed "Item Help" box on the right.
        TuiBox box = new TuiBox(startRow, startCol,
                                endCol - startCol + 1,
                                endRow - startRow + 1,
                                TuiBox.Style.SINGLE);
        box.titled("Item Help").border(HELP_FG).fill(-1);
        box.render(g, this);

        Row selected = getSelectedRow();
        if (selected == null || selected.help == null) return;

        drawWrappedText(g, selected.help,
                (startCol + 2) * CELL_W,
                (startRow + 1) * CELL_H,
                endCol - startCol - 3,
                HELP_FG);
    }

    private void drawWrappedText(GuiGraphics g, String text, int x, int y,
                                  int maxCols, int color) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = y;

        for (String word : words) {
            if (line.length() > 0
                    && line.length() + 1 + word.length() > maxCols) {
                drawDos(g, line.toString(), x, lineY, color);
                lineY += CELL_H;
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) {
            drawDos(g, line.toString(), x, lineY, color);
        }
    }

    private void drawFooter(GuiGraphics g) {
        int y = (rows() - 1) * CELL_H;
        g.fill(0, y, appWidth, y + CELL_H, FOOT_BG);

        String hints = " \u2191\u2193\u2192\u2190:Move  Enter:Select  " +
                       "+/-/PU/PD:Value  F10:Save  ESC:Exit  F1:General Help  " +
                       "F5:Previous  F6:Fail-Safe  F7:Optimized ";
        drawDos(g, hints, 0, y, FOOT_FG);
    }

    // Input

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // Modal dialogs take precedence over everything else.
        if (widgets.getFocus() != null) {
            return widgets.keyPressed(key, scan, mods);
        }

        switch (key) {
            case GLFW.GLFW_KEY_UP          -> { moveSelection(-1); return true; }
            case GLFW.GLFW_KEY_DOWN        -> { moveSelection(1);  return true; }
            case GLFW.GLFW_KEY_LEFT        -> { cycleTab(-1);      return true; }
            case GLFW.GLFW_KEY_RIGHT       -> { cycleTab(1);       return true; }
            case GLFW.GLFW_KEY_ENTER,
                 GLFW.GLFW_KEY_KP_ENTER    -> { activateSelected(); return true; }
            case GLFW.GLFW_KEY_PAGE_UP     -> { cycleValue(-1);    return true; }
            case GLFW.GLFW_KEY_PAGE_DOWN   -> { cycleValue(1);     return true; }
            case GLFW.GLFW_KEY_F1          -> {
                showMessage("General Help",
                        "Arrow keys navigate between items.",
                        "Left and right switch tabs.",
                        "Enter or +/- changes the highlighted value.",
                        "F10 saves and exits.",
                        "Esc exits without saving.");
                return true;
            }
            case GLFW.GLFW_KEY_F5 -> {
                // Restore the config as it was when SETUP opened.
                config = original;
                rebuildRows();
                selectFirstSelectable();
                return true;
            }
            case GLFW.GLFW_KEY_F6 -> {
                config = failSafeDefaults();
                rebuildRows();
                selectFirstSelectable();
                return true;
            }
            case GLFW.GLFW_KEY_F7 -> {
                config = optimizedDefaults();
                rebuildRows();
                selectFirstSelectable();
                return true;
            }
            case GLFW.GLFW_KEY_F10 -> { promptSaveAndExit(); return true; }
            case GLFW.GLFW_KEY_ESCAPE -> { promptExitWithoutSaving(); return true; }
        }
        return false;
    }

    @Override
    public boolean charTyped(char cp, int mods) {
        if (widgets.getFocus() != null) return widgets.charTyped(cp, mods);
        return false;
    }

    /** Move the selection by {@code delta} rows, skipping non-item rows. */
    private void moveSelection(int delta) {
        if (bodyRows.isEmpty()) return;
        int i = selectedRow;
        for (int step = 0; step < bodyRows.size(); step++) {
            i += delta;
            if (i < 0) i = bodyRows.size() - 1;
            if (i >= bodyRows.size()) i = 0;
            if (bodyRows.get(i).kind == Row.Kind.ITEM) {
                selectedRow = i;
                return;
            }
        }
    }

    /** Switch tabs and re-populate the body. */
    private void cycleTab(int delta) {
        Tab[] values = Tab.values();
        int idx = activeTab.ordinal() + delta;
        if (idx < 0) idx = values.length - 1;
        if (idx >= values.length) idx = 0;
        activeTab = values[idx];
        rebuildRows();
        selectFirstSelectable();
    }

    /** Handle Enter on the selected row. */
    private void activateSelected() {
        Row row = getSelectedRow();
        if (row == null || row.action == null) return;

        switch (row.action) {
            case "edit.date" -> showMessage("Date",
                    "Current value: " + awardDate(config.systemTime()),
                    "Editing individual date fields is not yet supported.");
            case "edit.time" -> showMessage("Time",
                    "Current value: " + awardTime(config.systemTime()),
                    "Editing individual time fields is not yet supported.");
            case "edit.floppyA" -> showFloppyPicker(config.floppyA(),
                    t -> config = config.withFloppyA(t));
            case "edit.floppyB" -> showFloppyPicker(config.floppyB(),
                    t -> config = config.withFloppyB(t));
            case "edit.hd1" -> showDiskPicker(config.hardDisk1(),
                    t -> config = config.withHardDisk1(t));
            case "edit.hd2" -> showDiskPicker(config.hardDisk2(),
                    t -> config = config.withHardDisk2(t));
            case "edit.display" -> showDisplayPicker();
            case "edit.coprocessor" -> {
                config = new MachineConfig(
                        config.systemTime(),
                        config.floppyA(), config.floppyB(),
                        config.hardDisk1(), config.hardDisk2(),
                        config.baseMemoryKb(), config.extendedMemoryKb(),
                        !config.mathCoprocessor(),
                        config.primaryDisplay());
                rebuildRows();
                selectFirstSelectable();
            }
            case "action.save"   -> promptSaveAndExit();
            case "action.exit"   -> promptExitWithoutSaving();
            case "action.defaults.optimized" -> {
                config = optimizedDefaults();
                rebuildRows();
                selectFirstSelectable();
            }
            case "action.defaults.failsafe" -> {
                config = failSafeDefaults();
                rebuildRows();
                selectFirstSelectable();
            }
        }
    }

    /** +/- or PgUp/PgDn on a value item cycles to the next enum value. */
    private void cycleValue(int delta) {
        Row row = getSelectedRow();
        if (row == null || row.action == null) return;

        switch (row.action) {
            case "edit.floppyA" -> {
                MachineConfig.FloppyType[] v = MachineConfig.FloppyType.values();
                int next = wrap(config.floppyA().ordinal() + delta, v.length);
                config = config.withFloppyA(v[next]);
                rebuildRows();
            }
            case "edit.floppyB" -> {
                MachineConfig.FloppyType[] v = MachineConfig.FloppyType.values();
                int next = wrap(config.floppyB().ordinal() + delta, v.length);
                config = config.withFloppyB(v[next]);
                rebuildRows();
            }
            case "edit.hd1" -> {
                MachineConfig.DiskType[] v = MachineConfig.DiskType.values();
                int next = wrap(config.hardDisk1().ordinal() + delta, v.length);
                config = config.withHardDisk1(v[next]);
                rebuildRows();
            }
            case "edit.hd2" -> {
                MachineConfig.DiskType[] v = MachineConfig.DiskType.values();
                int next = wrap(config.hardDisk2().ordinal() + delta, v.length);
                config = config.withHardDisk2(v[next]);
                rebuildRows();
            }
            case "edit.display" -> {
                MachineConfig.DisplayType[] v = MachineConfig.DisplayType.values();
                int next = wrap(config.primaryDisplay().ordinal() + delta, v.length);
                config = config.withPrimaryDisplay(v[next]);
                rebuildRows();
            }
            // action.* rows do not have values to cycle.
        }
    }

    private static int wrap(int i, int size) {
        int r = i % size;
        return r < 0 ? r + size : r;
    }

    // Pickers
    private void showFloppyPicker(MachineConfig.FloppyType current, Consumer<MachineConfig.FloppyType> onPick) {
        TuiDialog dlg = new TuiDialog()
                .addLine("")
                .addLine("Select Floppy Drive Type:")
                .addLine("");
        for (MachineConfig.FloppyType t : MachineConfig.FloppyType.values()) {
            String mark = (t == current) ? "* " : "  ";
            dlg.addItem(mark + t.displayName(), "pick:" + t.name());
        }
        dlg.onAction(a -> {
            dismissDialog(dlg);
            if (a.startsWith("pick:")) {
                onPick.accept(MachineConfig.FloppyType.valueOf(a.substring(5)));
                rebuildRows();
                selectFirstSelectable();
            }
        });
        dlg.onCancel(() -> dismissDialog(dlg));
        widgets.add(dlg);
        widgets.setFocus(dlg);
    }

    private void showDiskPicker(MachineConfig.DiskType current, Consumer<MachineConfig.DiskType> onPick) {
        TuiDialog dlg = new TuiDialog()
                .addLine("")
                .addLine("Select Hard Disk Type:")
                .addLine("");
        for (MachineConfig.DiskType t : MachineConfig.DiskType.values()) {
            String mark = (t == current) ? "* " : "  ";
            dlg.addItem(mark + t.displayName(), "pick:" + t.name());
        }
        dlg.onAction(a -> {
            dismissDialog(dlg);
            if (a.startsWith("pick:")) {
                onPick.accept(MachineConfig.DiskType.valueOf(a.substring(5)));
                rebuildRows();
                selectFirstSelectable();
            }
        });
        dlg.onCancel(() -> dismissDialog(dlg));
        widgets.add(dlg);
        widgets.setFocus(dlg);
    }

    private void showDisplayPicker() {
        TuiDialog dlg = new TuiDialog()
                .addLine("")
                .addLine("Select Video Display:")
                .addLine("");
        for (MachineConfig.DisplayType t : MachineConfig.DisplayType.values()) {
            String mark = (t == config.primaryDisplay()) ? "* " : "  ";
            dlg.addItem(mark + t.displayName(), "pick:" + t.name());
        }
        dlg.onAction(a -> {
            dismissDialog(dlg);
            if (a.startsWith("pick:")) {
                config = config.withPrimaryDisplay(
                        MachineConfig.DisplayType.valueOf(a.substring(5)));
                rebuildRows();
                selectFirstSelectable();
            }
        });
        dlg.onCancel(() -> dismissDialog(dlg));
        widgets.add(dlg);
        widgets.setFocus(dlg);
    }

    private void showMessage(String title, String... lines) {
        TuiDialog dlg = new TuiDialog()
                .addLine("")
                .addLine(title)
                .addLine("");
        for (String l : lines) dlg.addLine(l);
        dlg.addLine("");
        dlg.addItem("OK", "close");
        dlg.onAction(a -> dismissDialog(dlg));
        dlg.onCancel(() -> dismissDialog(dlg));
        widgets.add(dlg);
        widgets.setFocus(dlg);
    }

    private void dismissDialog(TuiDialog dlg) {
        widgets.remove(dlg);
        widgets.setFocus(null);
    }

    // Save / exit prompts
    private void promptSaveAndExit() {
        TuiDialog dlg = new TuiDialog()
                .addLine("")
                .addLine("Save to CMOS and EXIT (Y/N)?")
                .addLine("")
                .addItem("Yes", "yes")
                .addItem("No",  "no");
        dlg.onAction(a -> {
            dismissDialog(dlg);
            if (a.equals("yes")) saveAndExit();
        });
        dlg.onCancel(() -> dismissDialog(dlg));
        widgets.add(dlg);
        widgets.setFocus(dlg);
    }

    private void promptExitWithoutSaving() {
        TuiDialog dlg = new TuiDialog()
                .addLine("")
                .addLine("Quit without saving (Y/N)?")
                .addLine("")
                .addItem("Yes", "yes")
                .addItem("No",  "no");
        dlg.onAction(a -> {
            dismissDialog(dlg);
            if (a.equals("yes")) screen.returnToShell();
        });
        dlg.onCancel(() -> dismissDialog(dlg));
        widgets.add(dlg);
        widgets.setFocus(dlg);
    }

    private void saveAndExit() {
        ModMessages.sendToServer(
                new ServerboundSaveBiosConfigPacket(screen.getPos(), config));
        screen.returnToShell();
    }

    // Defaults
    private MachineConfig optimizedDefaults() {
        return MachineConfig.pentium4(System.currentTimeMillis());
    }

    private MachineConfig failSafeDefaults() {
        return new MachineConfig(
                System.currentTimeMillis(),
                MachineConfig.FloppyType.NONE,
                MachineConfig.FloppyType.NONE,
                MachineConfig.DiskType.NONE,
                MachineConfig.DiskType.NONE,
                640, 0,
                false,
                MachineConfig.DisplayType.MONO);
    }

    // Helpers
    private Row getSelectedRow() {
        if (selectedRow < 0 || selectedRow >= bodyRows.size()) return null;
        return bodyRows.get(selectedRow);
    }

    /** Format as Award BIOS prints it: {@code "Fri, Nov 18 2005"}. */
    private static String awardDate(long millis) {
        return new SimpleDateFormat("EEE, MMM dd yyyy").format(new Date(millis));
    }

    /** Format as Award BIOS prints it: {@code "12:34:56"}. */
    private static String awardTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(millis));
    }

    @Override
    public String getTitle() { return "Award BIOS SETUP"; }
}
