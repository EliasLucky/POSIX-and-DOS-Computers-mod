package com.eliaslucky.mc_dos.client;

import org.lwjgl.glfw.GLFW;

import com.eliaslucky.mc_dos.Computers;
import com.eliaslucky.mc_dos.blocks.computer.ComputerType;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.apps.TerminalApplicationRegistry;
import com.eliaslucky.mc_dos.network.ModMessages;
import com.eliaslucky.mc_dos.network.ServerboundCloseTerminalPacket;
import com.eliaslucky.mc_dos.network.ServerboundCommandPacket;
import com.eliaslucky.mc_dos.network.ServerboundFileWritePacket;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class ComputerTerminalScreen extends Screen {
	private static final ResourceLocation DOS_FONT = ResourceLocation.fromNamespaceAndPath(Computers.MODID, "ibm_vga_8x16");
	private static final Style DOS_STYLE = Style.EMPTY.withFont(DOS_FONT);

	private final BlockPos pos;
	private final ComputerType computerType;
	private TerminalApplication activeApp;
	private final List<String> history = new ArrayList<>();
	private final StringBuilder inputBuffer = new StringBuilder();
	private String activePath;

	private static final int MARGIN = 10;
	private static final int LINE_HEIGHT = 16;

	public ComputerTerminalScreen(BlockPos pos, ComputerType computerType) {
		super(Component.literal(computerType.modelName));
		this.pos = pos;
		this.computerType = computerType;
		this.activePath = computerType.defaultPath;

		history.add(computerType.biosString);
		history.add(computerType.memoryString);
		history.add(computerType.osVersion);
		history.add("");
		history.add(computerType.bootMessage);
		history.add("");
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		// EITHER LINE MODE OR TERMINAL APP MODE
		if (activeApp != null) {
			activeApp.setSize(this.width, this.height);
			activeApp.render(guiGraphics, mouseX, mouseY, partialTick);
			return;
		}

		guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);

		int textColor = computerType.textColor;
		int maxLineWidth = Math.max(50, this.width - (MARGIN * 2));

		List<FormattedCharSequence> wrappedLines = new ArrayList<>();
		for (String line : history) {
			if (line.isEmpty()) {
				wrappedLines.add(FormattedCharSequence.EMPTY);
			}
			else {
				wrappedLines.addAll(this.font.split(Component.literal(line).withStyle(DOS_STYLE), maxLineWidth));
			}
		}

		String prompt = computerType.commandProcessor.getPrompt(this.activePath);
		String cursor = ((System.currentTimeMillis() / 500) % 2 == 0) ? "_" : " ";
		String currentLine = prompt + inputBuffer.toString() + cursor;
		wrappedLines.addAll(this.font.split(Component.literal(currentLine).withStyle(DOS_STYLE), maxLineWidth));

		// auto-scroll window bounds based on screen height
		int maxVisibleLines = Math.max(1, (this.height - (MARGIN * 2)) / LINE_HEIGHT);
		int totalLines = wrappedLines.size();
		int startIndex = Math.max(0, totalLines - maxVisibleLines);

		int yOffset = MARGIN;
		for (int i = startIndex; i < totalLines; i++) {
			guiGraphics.drawString(this.font, wrappedLines.get(i), MARGIN, yOffset, textColor, false);
			yOffset += LINE_HEIGHT;
		}

		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (activeApp != null) return activeApp.charTyped(codePoint, modifiers);
		if (codePoint >= 32 && codePoint != 127) {
			inputBuffer.append(codePoint);
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (activeApp != null) {
			if (activeApp.keyPressed(keyCode, scanCode, modifiers)) return true;
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) { closeApp(); return true; }
			return false;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			String command = inputBuffer.toString().trim();
			String prompt = computerType.commandProcessor.getPrompt(this.activePath);
			history.add(prompt + command);
			
			executeCommand(command);
			
			inputBuffer.setLength(0);
			return true;
		} 
		else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && inputBuffer.length() > 0) {
			if (inputBuffer.length() > 0) {
				inputBuffer.deleteCharAt(inputBuffer.length() - 1);
			}
			return true;
		}
		else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int btn) {
		if (activeApp != null) return activeApp.mouseClicked(mx, my, btn);
		return super.mouseClicked(mx, my, btn);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double d) {
		if (activeApp != null) return activeApp.mouseScrolled(mx, my, d);
		return super.mouseScrolled(mx, my, d);
	}

	// APP STUFF
	public void launchApp(TerminalApplication app) {
		this.activeApp = app;
		app.setSize(this.width, this.height);
	}

	public void closeApp() {
		if (activeApp != null) {
			activeApp.onClose();
			activeApp = null;
		}
	}
	
	public void returnToShell() {
		if (activeApp != null) {
			activeApp.onClose();
			activeApp = null;
		}
	}

	public void saveFile(String path, String content) {
		ModMessages.sendToServer(new ServerboundFileWritePacket(this.pos, path, content));
	}

	private void executeCommand(String cmd) {
		if (cmd.isEmpty()) return;

		if (cmd.equalsIgnoreCase("CLEAR") || cmd.equalsIgnoreCase("CLS")) {
			history.clear();
			return;
		}

		ModMessages.sendToServer(new ServerboundCommandPacket(this.pos, cmd));
	}

	public void appendOutput(String output, String updatedPath) {
		if (updatedPath != null && !updatedPath.isEmpty()) this.activePath = updatedPath;
		if (output == null || output.isEmpty()) return;

		if (output.startsWith("APP_LAUNCH:")) {
			// APP_LAUNCH:NAME:ARGS:CONTENT  (split limit 4 keeps CONTENT intact)
			String[] parts = output.split(":", 4);
			String name    = parts.length > 1 ? parts[1] : "";
			String args    = parts.length > 2 ? parts[2] : "";
			String content = parts.length > 3 ? parts[3] : "";

			var factory = TerminalApplicationRegistry.get(name);
			if (factory != null) {
				launchApp(factory.create(this, new String[]{ args }, content));
			}
			else {
				history.add("Cannot launch app: " + name);
			}
			return;
		}

		for (String line : output.split("\n")) history.add(line);
	}

	public Font getDosFont()      { return this.font; }
	public Style getDosStyle()    { return DOS_STYLE; }
	public BlockPos getPos()      { return this.pos; }
	public ComputerType getType() { return this.computerType; }

	@Override
	public void onClose() {
		if (activeApp != null) { activeApp.onClose(); activeApp = null; }
		ModMessages.sendToServer(new ServerboundCloseTerminalPacket(this.pos));
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
