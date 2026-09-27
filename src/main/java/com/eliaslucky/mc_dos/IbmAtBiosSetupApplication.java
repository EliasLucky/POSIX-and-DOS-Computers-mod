package com.eliaslucky.mc_dos;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.tui.*;
import com.eliaslucky.mc_dos.network.ModMessages;
import com.eliaslucky.mc_dos.network.ServerboundSaveBiosConfigPacket;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class IbmAtBiosSetupApplication extends TerminalApplication {
    private final MachineConfig original;
    private MachineConfig config;
    private final TuiKeyValueTable table;
    private final TuiScreen widgets = new TuiScreen();
    private final String biosName;

    public IbmAtBiosSetupApplication(ComputerTerminalScreen screen, MachineConfig config, String biosName) {
        super(screen);
        this.original = config;
        this.config = config;
        this.biosName = biosName;

        this.table = new TuiKeyValueTable(3, 3, 62, 11);
        rebuildTable();
        table.onAction(this::editField);
        widgets.add(table);
        widgets.setFocus(table);
    }

    private void rebuildTable() {
        table.setRows(List.of(
            new TuiKeyValueTable.Row("Time",             config.timeString(),           true,  "edit.time"),
            new TuiKeyValueTable.Row("Date",             config.dateString(),           true,  "edit.date"),
            new TuiKeyValueTable.Row("Floppy Disk A:",   config.floppyA().displayName(),true,  "edit.floppyA"),
            new TuiKeyValueTable.Row("Floppy Disk B:",   config.floppyB().displayName(),true,  "edit.floppyB"),
            new TuiKeyValueTable.Row("Hard Disk 1 (C:)", config.hardDisk1().displayName(), true, "edit.hd1"),
            new TuiKeyValueTable.Row("Hard Disk 2 (D:)", config.hardDisk2().displayName(), true, "edit.hd2"),
            new TuiKeyValueTable.Row("Base Memory",      config.baseMemoryKb() + "K",   false, null),
            new TuiKeyValueTable.Row("Expansion Memory", config.extendedMemoryKb() + "K", false, null),
            new TuiKeyValueTable.Row("Math Coprocessor", config.mathCoprocessor() ? "Installed" : "Not installed", false, null),
            new TuiKeyValueTable.Row("Primary Display",  config.primaryDisplay().displayName(), true, "edit.display")
        ));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, appWidth, appHeight, TuiPalette.LIGHT_GRAY);

        // Title bar
        g.fill(0, 0, appWidth, CELL_H, TuiPalette.BLACK);
        drawDos(g, "[ " + biosName + " ]   [ Generic SETUP Version 3.0 2/88 ]",
                0, 0, TuiPalette.LIGHT_GRAY);

        // Framed table
        TuiBox frame = new TuiBox(2, 2, 64, 15, TuiBox.Style.SINGLE)
                .titled("Current SETUP Configuration");
        frame.render(g, this);

        // Table
        table.render(g, this);

        // Status footer
        int footY = (rows() - 1) * CELL_H;
        g.fill(0, footY, appWidth, appHeight, TuiPalette.BLACK);
        drawDos(g, "[ ESC ] Exit & Save   [ \u2191\u2193 ] Select   [ ENTER ] Change",
                0, footY, TuiPalette.LIGHT_GRAY);

        // Dialogs on top
        widgets.render(g, this);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (widgets.getFocus() instanceof TuiDialog) {
            return widgets.keyPressed(key, scan, mods);
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            saveAndExit();
            return true;
        }

        return table.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char cp, int mods) {
        return widgets.charTyped(cp, mods);
    }

    private void saveAndExit() {
        // Send the new config, then return to shell.
        ModMessages.sendToServer(new ServerboundSaveBiosConfigPacket(
                screen.getPos(), config));
        screen.returnToShell();
    }

    // Field editing
    private void editField(String action) {
        switch (action) {
            case "edit.floppyA" -> showFloppyPicker("A:", config.floppyA(), t -> {
                config = config.withFloppyA(t);
                rebuildTable();
            });
            case "edit.floppyB" -> showFloppyPicker("B:", config.floppyB(), t -> {
                config = config.withFloppyB(t);
                rebuildTable();
            });
            case "edit.display" -> showDisplayPicker();
            case "edit.time", "edit.date" -> showTextEditor(action);
            // ...
        }
    }

    private void showFloppyPicker(String label, MachineConfig.FloppyType current, Consumer<MachineConfig.FloppyType> onPick) {
        TuiDialog dlg = new TuiDialog()
                .addLine("")
                .addLine("Set " + label + " type:")
                .addLine("");
        for (MachineConfig.FloppyType t : MachineConfig.FloppyType.values()) {
            dlg.addItem(t.displayName(), "pick:" + t.name());
        }
        dlg.onAction(a -> {
            dismissDialog(dlg);
            if (a.startsWith("pick:")) {
                MachineConfig.FloppyType chosen =
                        MachineConfig.FloppyType.valueOf(a.substring(5));
                onPick.accept(chosen);
            }
        });
        dlg.onCancel(() -> dismissDialog(dlg));
        widgets.add(dlg);
        widgets.setFocus(dlg);
    }

    private void dismissDialog(TuiDialog dlg) {
        widgets.remove(dlg);
        widgets.setFocus(table);
    }

    // ...showDisplayPicker, showTextEditor...

    @Override
    public String getTitle() { return "BIOS SETUP"; }
}
