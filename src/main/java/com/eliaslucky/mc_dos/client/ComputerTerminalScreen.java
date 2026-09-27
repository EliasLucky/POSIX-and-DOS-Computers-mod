package com.eliaslucky.mc_dos.client;

import org.lwjgl.glfw.GLFW;

import com.eliaslucky.mc_dos.Computers;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.blocks.computer.MachineType;
import com.eliaslucky.mc_dos.client.apps.FileAwareApp;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.apps.TerminalApplicationRegistry;
import com.eliaslucky.mc_dos.client.apps.bios.BiosSetupRegistry;
import com.eliaslucky.mc_dos.network.ModMessages;
import com.eliaslucky.mc_dos.network.ServerboundCloseTerminalPacket;
import com.eliaslucky.mc_dos.network.ServerboundCommandPacket;
import com.eliaslucky.mc_dos.network.ServerboundFileWritePacket;
import com.eliaslucky.mc_dos.network.ServerboundRequestBiosConfigPacket;
import com.eliaslucky.mc_dos.network.ServerboundSkipPostPacket;

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
	private final MachineType MachineType;
	private TerminalApplication activeApp;
	private final List<String> history = new ArrayList<>();
	private final StringBuilder inputBuffer = new StringBuilder();
	private String activePath;
	private boolean postPhase = false;
	private long    postStartMillis = 0;
	private int     postCountdownSeconds = 5;
	private static final int MARGIN = 10;
	private static final int LINE_HEIGHT = 16;

	public ComputerTerminalScreen(BlockPos pos, MachineType MachineType) {
		super(Component.literal(MachineType.modelName()));
		this.pos = pos;
		this.MachineType = MachineType;
		this.activePath = MachineType.defaultPath();
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

		int textColor = MachineType.textColor();
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
		if (!postPhase) {
			String prompt = MachineType.commandProcessor().getPrompt(this.activePath);
			String cursor = ((System.currentTimeMillis() / 500) % 2 == 0) ? "_" : " ";
			String currentLine = prompt + inputBuffer.toString() + cursor;
			wrappedLines.addAll(this.font.split(Component.literal(currentLine).withStyle(DOS_STYLE), maxLineWidth));
		}
		// auto-scroll window bounds based on screen height
		int maxVisibleLines = Math.max(1, (this.height - (MARGIN * 2)) / LINE_HEIGHT);
		int totalLines = wrappedLines.size();
		int startIndex = Math.max(0, totalLines - maxVisibleLines);

		int yOffset = MARGIN;
		for (int i = startIndex; i < totalLines; i++) {
			guiGraphics.drawString(this.font, wrappedLines.get(i), MARGIN, yOffset, textColor, false);
			yOffset += LINE_HEIGHT;
		}
		if (postPhase) {
			long elapsed = System.currentTimeMillis() - postStartMillis;
			int remaining = postCountdownSeconds - (int)(elapsed / 1000);
			if (remaining <= 0) {
				postPhase = false;
				ModMessages.sendToServer(new ServerboundSkipPostPacket(this.pos));
			} else {
				Component msg = Component.literal("Press DEL to enter SETUP ... " + remaining)
				        .withStyle(DOS_STYLE);
				int w = this.font.width(msg);
				int y = this.height - MARGIN - LINE_HEIGHT;
				guiGraphics.drawString(this.font, msg, this.width - MARGIN - w, y, textColor, false);
			}
		}
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (activeApp != null) return activeApp.charTyped(codePoint, modifiers);
		if (postPhase) return true;
		if (codePoint >= 32 && codePoint != 127) {
			inputBuffer.append(codePoint);
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (postPhase) {
		    if (keyCode == GLFW.GLFW_KEY_DELETE) {
		        postPhase = false;
		        ModMessages.sendToServer(new ServerboundRequestBiosConfigPacket(pos));
		        return true;
		    }
		    postPhase = false;
		    ModMessages.sendToServer(new ServerboundSkipPostPacket(pos));
		}
		if (activeApp != null) {
			if (activeApp.keyPressed(keyCode, scanCode, modifiers)) return true;
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) { closeApp(); return true; }
			return false;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			String command = inputBuffer.toString().trim();
			String prompt = MachineType.commandProcessor().getPrompt(this.activePath);
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
	/**
	 * Called by a {@link TerminalApplication} when the user exits the
	 * app and wants to return to the shell prompt.
	 *
	 * <p>Unlike {@link #onClose()}, this does <em>not</em> send the
	 * close packet to the server. The player is still occupying the
	 * terminal; only the running app is dismissed. Command history
	 * stays visible and the shell is immediately interactive again.
	 */
	public void returnToShell() {
		if (activeApp != null) {
			activeApp.onClose();
			activeApp = null;
		}
	}

	public void saveFile(String path, String content) {
		ModMessages.sendToServer(new ServerboundFileWritePacket(this.pos, path, content));
	}
	/**
	 * Called by {@link com.eliaslucky.mc_dos.network.ClientboundFileWriteResultPacket}
	 * when the server finishes processing a save.
	 *
	 * @param path    the path that was written
	 * @param success whether the write succeeded
	 * @param message error message, empty on success
	 */
	public void onFileWriteResult(String path, boolean success, String message) {
	    if (activeApp instanceof FileAwareApp fa) {
	        fa.onFileWriteResult(path, success, message);
	    }
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

		if (output.equals("__CLEAR__")) { history.clear(); return; }
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
	/**
	 * Called by {@link ClientboundTerminalStatePacket} when the server
	 * tells the client what phase the machine is in.
	 *
	 * <p>When the machine is in POST, the client clears its history,
	 * seeds it with the BIOS's POST lines, and starts the SETUP
	 * countdown. When the machine is already running, the client just
	 * clears the countdown flag and leaves the history alone — the
	 * shell will populate it via normal command execution.
	 *
	 * @param postPhase       whether the machine is showing POST
	 * @param postLines       BIOS output lines, empty when not in POST
	 * @param countdownSeconds how long the DEL prompt stays visible
	 */
	public void onTerminalState(boolean postPhase, List<String> postLines, int countdownSeconds) {
	    this.postPhase = postPhase;
	    this.postStartMillis = System.currentTimeMillis();
	    this.postCountdownSeconds = countdownSeconds;
	    if (postPhase) {
	        history.clear();
	        for (String line : postLines) history.add(line);
	        history.add("");
	    }
	}

	/**
	 * Called by {@link ClientboundBiosConfigPacket} when the server
	 * responds to a SETUP request. Looks up the setup screen by ID in
	 * {@link BiosSetupRegistry}. Addons that ship their own BIOS register
	 * their setup screen under the same ID that their BIOS's
	 * {@code setupScreenId()} returns.
	 */
	public void onBiosConfigReceived(MachineConfig config, String biosName, String setupScreenId) {
	    var factory = BiosSetupRegistry.get(setupScreenId);
	    if (factory == null) {
	        history.add("No setup screen registered for BIOS: " + setupScreenId);
	        return;
	    }
	    launchApp(factory.create(this, config, biosName));
	}
	public Font getDosFont()      { return this.font; }
	public Style getDosStyle()    { return DOS_STYLE; }
	public BlockPos getPos()      { return this.pos; }
	public MachineType getType() { return this.MachineType; }

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
