package com.eliaslucky.mc_dos.client.apps.bios;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.tui.*;
import com.eliaslucky.mc_dos.network.ModMessages;
import com.eliaslucky.mc_dos.network.ServerboundBootActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

public class IbmAtBiosSetupApplication extends TerminalApplication {
	private final MachineConfig original;
	private MachineConfig config;
	private final TuiKeyValueTable table;
	private final TuiScreen widgets = new TuiScreen();
	private final String biosName;

	private List<TuiKeyValueTable.Row> currentRows = List.of();

	public IbmAtBiosSetupApplication(ComputerTerminalScreen screen, MachineConfig config, String biosName) {
		super(screen);
		this.original = config;
		this.config = config;
		this.biosName = biosName;

		this.table = new TuiKeyValueTable(0, 0, 1, 1);
		rebuildTable();
		table.onAction(this::editField);
	}

	private void rebuildTable() {
		currentRows = List.of(
			new TuiKeyValueTable.Row("Time",			 config.timeString(),			true,  "edit.time"),
			new TuiKeyValueTable.Row("Date",			 config.dateString(),			true,  "edit.date"),
			new TuiKeyValueTable.Row("Floppy Disk A:",	 config.floppyA().displayName(),true,  "edit.floppyA"),
			new TuiKeyValueTable.Row("Floppy Disk B:",	 config.floppyB().displayName(),true,  "edit.floppyB"),
			new TuiKeyValueTable.Row("Hard Disk 1 (C:)", config.hardDisk1().displayName(), true, "edit.hd1"),
			new TuiKeyValueTable.Row("Hard Disk 2 (D:)", config.hardDisk2().displayName(), true, "edit.hd2"),
			new TuiKeyValueTable.Row("Base Memory",		 config.baseMemoryKb() + "K",	false, null),
			new TuiKeyValueTable.Row("Expansion Memory", config.extendedMemoryKb() + "K", false, null),
			new TuiKeyValueTable.Row("Math Coprocessor", config.mathCoprocessor() ? "Installed" : "Not installed", false, null),
			new TuiKeyValueTable.Row("Primary Display",  config.primaryDisplay().displayName(), true, "edit.display")
		);
		table.setRows(currentRows);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		final int W = cols();
		final int H = rows();
		final int fg = TuiPalette.WHITE;
		final int bg = TuiPalette.BLACK;

		g.fill(0, 0, appWidth, appHeight, bg);

		String shadeRow = "\u2592".repeat(Math.max(0,W-2));
		for (int r = 1; r < H-1; r++) {
			drawDos(g,shadeRow,CELL_W,r*CELL_H,fg);
		}

		// Outer frame
		StringBuilder top = new StringBuilder(W);
		top.append('\u250C');
		for (int i = 0; i < W-2; i++) top.append('\u2500');
		top.append('\u2510');
		drawDos(g, top.toString(),0,0,fg);

		// Row H-1
		StringBuilder bot = new StringBuilder(W);
		bot.append('\u2514');
		for (int i = 0; i < W-2; i++) bot.append('\u2500');
		bot.append('\u2518');
		drawDos(g, bot.toString(),0,(H-1)*CELL_H,fg);

		// Verticals on rows 1..H-2
		for (int r=1; r < H-1; r++) {
			drawDos(g, "\u2502", 0, r*CELL_H,fg);
			drawDos(g, "\u2502", (W-1)*CELL_W,r*CELL_H,fg);
		}

		String leftText = "[ F1 HELP ]";
		String centerText = "[ Generic SETUP version 3.0 Z/B8 ]";
		String rightText = "[80286]";

		int innerW = W-2;
		int leftStart = 2;
		int centerStart = 1+(innerW - centerText.length())/2;
		int rightStart = W-2-rightText.length();
		g.fill(leftStart * CELL_W, CELL_H, (leftStart + leftText.length()) * CELL_W,2*CELL_H,bg);
		g.fill(centerStart * CELL_W, CELL_H, (centerStart + centerText.length()) * CELL_W,2*CELL_H,bg);
		g.fill(rightStart * CELL_W,CELL_H, (rightStart + rightText.length()) * CELL_W, 2*CELL_H,bg);

		drawDos(g, leftText, leftStart * CELL_W, CELL_H, fg);
		drawDos(g, centerText, centerStart * CELL_W, CELL_H, fg);
		drawDos(g, rightText, rightStart * CELL_W, CELL_H ,fg);

		// Body shade fill
		//String shade = "\u2592".repeat(Math.max(0,W-2));
		//for (int r=2; r<H-1;r++) {
		//	drawDos(g,shade,CELL_W,r*CELL_H,fg);
		//}
		
		// Double-bordered central box
		renderBox(g, W);

		// Dialogs on top
		widgets.render(g, this);
	}

	private void renderBox(GuiGraphics g, int W) {
		final int fg = TuiPalette.WHITE;
		final int bg = TuiPalette.BLACK;

		int boxW = Math.min(68,W-8);
		int boxH = 13;
		int boxCol = (W-boxW)/2;
		int boxRow = 3;

		g.fill(boxCol*CELL_W, boxRow * CELL_H,
		       (boxCol+boxW)*CELL_W, (boxRow + boxH) * CELL_H,
		       bg);
		int innerW = boxW -2;

		String title = " Current SETUP Configuration ";
		int titleStart = Math.max(0,(innerW-title.length())/2);
		StringBuilder top = new StringBuilder(boxW);
		top.append('\u2554');
		for (int i = 0; i < innerW; i++) {
			if (i >= titleStart && i < titleStart + title.length()) {
				top.append(title.charAt(i-titleStart));
			}
			else {
				top.append('\u2550');
			}
		}
		top.append('\u2557');
		drawDos(g,top.toString(),boxCol *CELL_W, boxRow*CELL_H,fg);

		// Bottom border
		StringBuilder bot = new StringBuilder(boxW);
		bot.append('\u255A');
		for (int i = 0; i < innerW; i++) bot.append('\u2550');
		bot.append('\u255D');
		drawDos(g,bot.toString(),boxCol*CELL_W,(boxRow+boxH-1)*CELL_H,fg);

		// Side borders
		for (int r = 1; r < boxH - 1; r++) {
			int y = (boxRow+r)*CELL_H;
			drawDos(g, "\u2551",boxCol*CELL_W,y,fg);
			drawDos(g, "\u2551",(boxCol+boxW-1)*CELL_W,y,fg);
		}

		// divider column counted from the first interior cell.
		// 42 of 66 puts it at ~63% across. which makes it seem like it's closer to the right side
		int divInner = 42;

		// Single-rule dash row. One space at each end (so the dashes don't touch the double border)
		// and one at the divider column (so the vertica line below "meets" it with a gap)
		StringBuilder dash = new StringBuilder(innerW);
		for (int i = 0; i < innerW; i++) {
			if (i==0 || i==innerW-1 || i == divInner) dash.append(' ');
			else dash.append('\u2500');
		}
		drawDos(g,dash.toString(),(boxCol+1)*CELL_W,(boxRow+1)*CELL_H,fg);

		// Data rows
		int sel = table.selectedIndex();
		int lastCol = boxCol + boxW-2;
		int minVal = boxCol + 1 + divInner + 2;

		for (int i = 0; i < currentRows.size(); i++) {
			TuiKeyValueTable.Row row = currentRows.get(i);
			int r = boxRow +2+i;
			if (r >= boxRow + boxH-1) break;

			int y = r*CELL_H;
			boolean hot = (i == sel);
			
			g.fill((boxCol+1) * CELL_W,y,
				(boxCol+boxW-1) * CELL_W,y+CELL_H,
				hot ? fg : bg);
			
			int textFg = hot ? bg : fg;
			
			// Left column: "[N] Key"
			drawDos(g, "[" + i + "] " + row.key(),(boxCol+1)*CELL_W,y,textFg);
			// Single-rule vertical split
			drawDos(g, "\u2502", (boxCol+1+divInner)*CELL_W,y,textFg);
			// Right column: value, right-aligned to the interior edge
			String value = row.value();
			int valueStart = lastCol - value.length() + 1;
			if (valueStart < minVal) valueStart = minVal;
			drawDos(g,value,valueStart * CELL_W, y, textFg);
		}
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (!widgets.widgets().isEmpty()) {
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
		ModMessages.sendToServer(new ServerboundBootActionPacket(
				screen.getPos(), ServerboundBootActionPacket.Action.SAVE_BIOS, config));
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

	private void showDisplayPicker() {
		TuiDialog dlg = new TuiDialog().addLine("").addLine("Set display:").addLine("");
		for (MachineConfig.DisplayType t : MachineConfig.DisplayType.values()) {
			dlg.addItem(t.displayName(), "pick:" + t.name());
		}
		dlg.onAction(a -> {
			dismissDialog(dlg);
			if (a.startsWith("pick:")) {
				config = config.withPrimaryDisplay(
						MachineConfig.DisplayType.valueOf(a.substring(5)));
				rebuildTable();
			}
		});
		dlg.onCancel(() -> dismissDialog(dlg));
		widgets.add(dlg);
		widgets.setFocus(dlg);
	}

	private void showTextEditor(String action) {
		showMessage("Not implemented", "Press ESC to close");
	}
	private void showMessage(String title, String body) {
		TuiDialog dlg = new TuiDialog().addLine("").addLine(body).addLine("").addItem("OK", "close");
		dlg.onAction(a -> dismissDialog(dlg));
		dlg.onCancel(() -> dismissDialog(dlg));
		widgets.add(dlg);
		widgets.setFocus(dlg);
	}
	@Override
	public String getTitle() { return "BIOS SETUP"; }
}
