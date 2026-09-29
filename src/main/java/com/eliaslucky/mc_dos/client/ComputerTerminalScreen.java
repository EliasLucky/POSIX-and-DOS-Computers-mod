package com.eliaslucky.mc_dos.client;

import org.lwjgl.glfw.GLFW;

import com.eliaslucky.mc_dos.Computers;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.blocks.computer.MachineType;
import com.eliaslucky.mc_dos.client.apps.FileAwareApp;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.apps.TerminalApplicationRegistry;
import com.eliaslucky.mc_dos.client.apps.bios.BiosSetupRegistry;
import com.eliaslucky.mc_dos.network.ClientboundTerminalStatePacket;
import com.eliaslucky.mc_dos.network.ModMessages;
import com.eliaslucky.mc_dos.network.ServerboundCloseTerminalPacket;
import com.eliaslucky.mc_dos.network.ServerboundCommandPacket;
import com.eliaslucky.mc_dos.network.ServerboundFileWritePacket;
import com.eliaslucky.mc_dos.network.ServerboundBootActionPacket;

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

	private boolean postPhase = true;
	private boolean countdownActive = false;
	private boolean skipRequested = false;
	private long countdownEndMillis = 0;
	
	private static final int MARGIN = 10;
	private static final int LINE_HEIGHT = 16;

	public ComputerTerminalScreen(BlockPos pos, MachineType MachineType) {
		super(Component.literal(MachineType.modelName()));
		this.pos = pos;
		this.MachineType = MachineType;
		this.activePath = MachineType.defaultPath();
		ModMessages.sendToServer(new ServerboundBootActionPacket(pos, ServerboundBootActionPacket.Action.REQUEST_STATE));
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

		int remaining = remainingSeconds();
		if (postPhase && countdownActive && remaining == 0 && !skipRequested) {
			skipRequested = true;
			countdownActive = false;
			ModMessages.sendToServer(new ServerboundBootActionPacket(this.pos, ServerboundBootActionPacket.Action.SKIP_POST));
		}

		int lastPromptIndex = -1;
		if (postPhase && countdownActive && remaining > 0) {
			for (int i = history.size()-1; i >= 0; i--) {
				String line = history.get(i);
				if (!line.isEmpty() && line.toLowerCase().contains("press")) {
					lastPromptIndex = i;
					break;
				}
			}
		}

		List<FormattedCharSequence> wrappedLines = new ArrayList<>();
		for (int i = 0; i < history.size(); i++) {
			String line = history.get(i);

			if (i == lastPromptIndex) {	
				line = line + " ... " + remaining;	
			}
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
			if (skipRequested) return true;
			skipRequested = true;
			countdownActive = false;
			if (keyCode == GLFW.GLFW_KEY_DELETE) {
				ModMessages.sendToServer(new ServerboundBootActionPacket(pos,ServerboundBootActionPacket.Action.ENTER_SETUP));
			}
			else {
				ModMessages.sendToServer(new ServerboundBootActionPacket(pos,ServerboundBootActionPacket.Action.SKIP_POST));
			}
			return true;
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
			inputBuffer.deleteCharAt(inputBuffer.length() - 1);
			return true;
		}
		else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (activeApp != null) {
			if (activeApp.keyReleased(keyCode,scanCode,modifiers)) return true;
		}
		return super.keyReleased(keyCode,scanCode,modifiers);
	}

	// TODO: FOR THE FUTURE when i am gonna add GUI that relies on mouse
	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (activeApp != null) {
			if (activeApp.mouseReleased(mx,my,button)) return true;
		}
		return super.mouseReleased(mx,my,button);
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
	 * @param path	  the path that was written
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
	 * <p>The packet carries only the fields relevant to the current phase:
	 * <ul>
	 *   <li>{@code POST} - {@code postLines} and {@code countdownSeconds} are populated.</li>
	 *   <li>{@code RUNNING} - all phase-specific fields are null.</li>
	 *   <li>{@code SETUP} - {@code biosConfig}, @{code biosName}, and {@code setupScreenId} are populated.</li>
	 *
	 * @param phase            which moed the machine is in
	 * @param currentPath      the working directory (always present)
	 * @param postLines        BIOS POST lines (POST)
	 * @param countdownSeconds DEL countdown duration (POST)
	 * @param biosConfig	   machine configuration (SETUP)
	 * @param biosName         BIOS display name (SETUP)
	 * @param setupScreenId    setup screen registry (SETUP)
	 */
	public void onTerminalState(ClientboundTerminalStatePacket.Phase phase, String currentPath, List<String> postLines, int countdownSeconds, MachineConfig biosConfig, String biosName, String setupScreenId) {
		if (currentPath != null && !currentPath.isEmpty()) {
			this.activePath = currentPath;
		}
		switch (phase) {
			case POST -> {
				boolean wasInPost = this.postPhase;
				this.postPhase = true;
				if (!wasInPost || !countdownActive) {
					this.countdownEndMillis = System.currentTimeMillis() + countdownSeconds * 1000L;
					this.countdownActive = true;
					this.skipRequested = false;
				}
				history.clear();
				for (String line : postLines) history.add(line);
				history.add("");
			}
			case RUNNING  -> {
				this.postPhase = false;
				this.countdownActive = false;
				this.skipRequested = false;
			}
			case SETUP -> {
				this.postPhase = false;
				this.countdownActive = false;
				this.skipRequested = false;
				var factory = BiosSetupRegistry.get(setupScreenId);
				if (factory != null) {
					launchApp(factory.create(this,biosConfig,biosName));
				}
				else {
					history.add("No setup screen registered for BIOS: " + setupScreenId);
				}
			}
		}
	}

	private int remainingSeconds() {
		if (!countdownActive) return -1;
		long left = countdownEndMillis - System.currentTimeMillis();
		if (left <= 0) return 0;
		return (int)Math.ceil(left/1000.0);
	}	
	public Font getDosFont()	  { return this.font; }
	public Style getDosStyle()	  { return DOS_STYLE; }
	public BlockPos getPos()	  { return this.pos; }
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
