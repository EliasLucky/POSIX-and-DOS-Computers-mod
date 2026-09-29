package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.blocks.computer.basic.QBasicInterpreter;
import com.eliaslucky.mc_dos.blocks.computer.basic.RunState;
import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.FileAwareApp;
import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import com.eliaslucky.mc_dos.client.apps.display.Screen0Text;
import com.eliaslucky.mc_dos.client.apps.editor.AbstractEditorApplication;
import com.eliaslucky.mc_dos.client.apps.editor.DialogState;
import com.eliaslucky.mc_dos.client.tui.TuiDialog;
import com.eliaslucky.mc_dos.client.tui.TuiMenu;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class QBasicApplication extends AbstractEditorApplication implements FileAwareApp {
	public enum Mode  { EDITOR, MENU, DIALOG, RUN_OUTPUT,RUNNING }
	public enum Focus { EDIT, IMMEDIATE }

	private Mode  mode	= Mode.EDITOR;
	private Focus focus = Focus.EDIT;
	private RunState runState = RunState.FINISHED;
	private final StringBuilder inputBuffer = new StringBuilder();

	// Regions
	private final TuiMenu		menuBar;
	private final ImmediatePane immediate = new ImmediatePane();

	// Interpreter (built on each F5 run)
	private QBasicInterpreter interpreter;
	private QBasicHostImpl	  host;

	private boolean altHeld = false;
	private boolean consumingMenuKeystroke = false;
	private boolean pendingExit = false;
	private boolean keyEnqueuedInKeyPressed = false;

	// Snapshot of the source so we can restore the editor after the run.
	private String pendingSourceSnapshot;

	public QBasicApplication(ComputerTerminalScreen screen, String[] args, String initialContent) {
		super(screen,
			  args.length > 0 && !args[0].isEmpty() ? args[0] : "Untitled",
			  initialContent);
		
		this.menuBar = new TuiMenu(0, QBasicMenus.ROOT);
		this.menuBar.onAction(this::invokeMenuAction);
		
		if (initialContent == null || initialContent.isEmpty()) {
			showWelcomeDialog();
			this.mode	= Mode.DIALOG;
		}
	}

	private static int letterFromKey(int key) {
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return key;
		return -1;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (pendingExit) {
			pendingExit = false;
			screen.returnToShell();
			return;
		}
		if (mode == Mode.RUNNING) {
			if (runState != RunState.WAITING_INPUT) {
				runState = interpreter.tick(QBasicInterpreter.STEPS_PER_TICK);
			}
			if (runState == RunState.FINISHED) {
				mode = Mode.RUN_OUTPUT;
				// fall through to render the output normally
			}
			// Otherwise render the display below.
		}
		if (mode == Mode.RUNNING || mode == Mode.RUN_OUTPUT) {
			g.fill(0, 0, appWidth, appHeight, DosPalette.BLACK);
			displayMode.render(g, 0, 0, appWidth, appHeight);
			renderFooter(g);
			menuBar.renderOverlay(g, this);
			return;
		}

		g.fill(0, 0, appWidth, appHeight, theme.screenBg());
		renderMenuBar(g);
		renderHeader(g);
		renderEditorPane(g);
		renderImmediateContent(g);
		renderDivider(g);
		renderFooter(g);
		overlay.render(g, this);
		menuBar.renderOverlay(g, this);
	}
	
	@Override
	protected void renderImmediateContent(GuiGraphics g) {
		immediate.render(g, this, 0, immediateRow(), cols(),focus == Focus.IMMEDIATE);
	}
	
	@Override
	protected void renderHeader(GuiGraphics g) {
		int w = cols();
		String title = " " + filePath + " ";
		int pad = (w - title.length()) / 2;
		StringBuilder left	= new StringBuilder();
		StringBuilder right = new StringBuilder();
		for (int i = 0; i < pad; i++) left.append('\u2500');		   // ─
		for (int i = 0; i < w - pad - title.length(); i++) right.append('\u2500');

		int y = CELL_H;												   // row 1
		drawDos(g, left.toString(),  0, y, DosPalette.LIGHT_GRAY);
		drawDos(g, title,			 pad * CELL_W, y, DosPalette.WHITE);
		drawDos(g, right.toString(), (pad + title.length()) * CELL_W, y, DosPalette.LIGHT_GRAY);
	}

	@Override
	protected void renderMenuBar(GuiGraphics g) {
		menuBar.render(g, this);
	}

	@Override
	protected String footerHints() {
		return switch (mode) {
			case MENU		-> menuBar.footerHelp();
			case DIALOG		-> " F1=Help  Enter=Execute  Esc=Cancel  Tab=Next Field  Arrow=Next Item ";
			case RUN_OUTPUT -> " Press any key to continue ";
			case EDITOR		-> (focus == Focus.IMMEDIATE)
					? " Enter=Execute  F6=Editor  Esc=Cancel "
					: " F1=Help  F2=Save  F5=Run  F6=Window  F10=Menu ";
			case RUNNING -> switch (runState) {
				case WAITING_INPUT -> " Type your input, ENTER to submit, ESC to abort ";
				case WAITING_SLEEP -> " Sleeping...  ESC to abort ";
				case FINISHED	   -> " Press any key to continue ";
				case RUNNING	   -> " Running...	ESC to abort ";
			};
		};
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		boolean isAltKey = (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT);
		boolean altMod	 = (mods & GLFW.GLFW_MOD_ALT) != 0;

		if (isAltKey) {
			if (!altHeld) {
				altHeld = true;
				if (mode == Mode.EDITOR) {
					menuBar.open();
					mode = Mode.MENU;
				}
				else if (mode == Mode.MENU) {
					menuBar.close();
					mode = Mode.EDITOR;
				}
			}
			return true;
		}

		if (altMod) {
			int letter = letterFromKey(key);
			if (letter >= 0 && menuBar.openByMnemonic((char) letter)) {
				mode = Mode.MENU;
				return true;
			}
			return false;
		}

		switch (mode) {
			case RUNNING:
				// Abort at any time.
				if (key == GLFW.GLFW_KEY_ESCAPE) {
					interpreter.stop();
					runState = RunState.FINISHED;
					mode = Mode.RUN_OUTPUT;
					return true;
				}
	
				if (runState == RunState.WAITING_INPUT) {
					if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
						host.print("\n");
						interpreter.provideInput(inputBuffer.toString());
						inputBuffer.setLength(0);
						runState = RunState.RUNNING;
						return true;
					}
					if (key == GLFW.GLFW_KEY_BACKSPACE && inputBuffer.length() > 0) {
						inputBuffer.deleteCharAt(inputBuffer.length() - 1);
						host.backspaceChar();
						return true;
					}
				}
				if (host != null) {
					String arrow = arrowKeyString(key);
					if (arrow != null) {
						host.enqueueKey(arrow);
					}
					else if (runState != RunState.WAITING_INPUT) {
						char c = keyCodeToChar(key,mods);
						if (c != 0) {
							host.enqueueKey(String.valueOf(c));
							keyEnqueuedInKeyPressed = true;
						}
					}
				}
				return true;
			case RUN_OUTPUT:
				mode = Mode.EDITOR;
				restoreEditorAfterRun();
				return true;

			case MENU: {
				if (menuBar.keyPressed(key, scan, mods)) {
					if (isPrintableKey(key)) consumingMenuKeystroke = true;
					if (!menuBar.isOpen() && mode == Mode.MENU) mode = Mode.EDITOR;
					return true;
				}
				return true;
			}

			case DIALOG:
				if (overlay.keyPressed(key, scan, mods)) return true;
				return true;

			case EDITOR:
				if (overlay.keyPressed(key, scan, mods)) return true;
				if (key == GLFW.GLFW_KEY_F10) {
					menuBar.open();
					mode = Mode.MENU;
					return true;
				}
				if (key == GLFW.GLFW_KEY_TAB) {
					if (focus == Focus.EDIT) {
						insertTab();
						return true;
					}
					immediate.insertTab();
					return true;
				}
				if (focus == Focus.IMMEDIATE) {
					return handleImmediateKey(key);
				}
				return super.keyPressed(key, scan, mods);
		}
		return false;
	}
	private static char keyCodeToChar(int key, int mods) {
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
			char c = (char) ('a' + (key - GLFW.GLFW_KEY_A));
			if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) c = Character.toUpperCase(c);
			return c;
		}
		if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
			if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) return ")!@#$%^&*(".charAt(key-GLFW.GLFW_KEY_0);
			return (char) ('0' + (key - GLFW.GLFW_KEY_0));
		}
		if (key == GLFW.GLFW_KEY_SPACE) return ' ';
		return 0;
	}
	/**
	 * Maps a GLFW arrow key to the two-character sequence QBasic's
	 * {@code INKEY$} returns for that key: {@code CHR$(0)} followed by a
	 * letter. Programs that read arrow keys test for exactly this shape.
	 *
	 * @param key a {@code GLFW_KEY_*} constant
	 * @return the QBasic keycode, or {@code null} if the key isn't an arrow
	 */
	private static String arrowKeyString(int key) {
		return switch (key) {
			case GLFW.GLFW_KEY_UP	 -> "\u0000H";
			case GLFW.GLFW_KEY_DOWN  -> "\u0000P";
			case GLFW.GLFW_KEY_LEFT  -> "\u0000K";
			case GLFW.GLFW_KEY_RIGHT -> "\u0000M";
			default -> null;
		};
	}

	@Override
	public boolean keyReleased(int key, int scan, int mods) {
		if (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT) {
			altHeld = false;
		}
		return super.keyReleased(key, scan, mods);
	}

	@Override
	protected boolean handleFunctionKey(int key) {
		switch (key) {
			case GLFW.GLFW_KEY_F2: saveFile(); return true;
			case GLFW.GLFW_KEY_F5: startRun(); return true;
			case GLFW.GLFW_KEY_F6: focus = (focus == Focus.EDIT) ? Focus.IMMEDIATE : Focus.EDIT; return true;
		}
		return false;
	}

	// Immediate pane
	private boolean handleImmediateKey(int key) {
		switch (key) {
			case GLFW.GLFW_KEY_F6:		  focus = Focus.EDIT;	 return true;
			case GLFW.GLFW_KEY_BACKSPACE: immediate.backspace(); return true;
			case GLFW.GLFW_KEY_LEFT:	  immediate.moveLeft();  return true;
			case GLFW.GLFW_KEY_RIGHT:	  immediate.moveRight(); return true;
			case GLFW.GLFW_KEY_UP:		  immediate.moveUp();	 return true;
			case GLFW.GLFW_KEY_DOWN:	  immediate.moveDown();  return true;
			case GLFW.GLFW_KEY_TAB:		  immediate.insertTab(); return true;
			case GLFW.GLFW_KEY_ENTER:
			case GLFW.GLFW_KEY_KP_ENTER:
				executeImmediateLine(immediate.consume());
				return true;
		}
		return false;
	}
	
	private static boolean isPrintableKey(int key) {
		return (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z)
			|| (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9)
			|| key == GLFW.GLFW_KEY_SPACE
			|| key == GLFW.GLFW_KEY_MINUS
			|| key == GLFW.GLFW_KEY_EQUAL
			|| key == GLFW.GLFW_KEY_LEFT_BRACKET
			|| key == GLFW.GLFW_KEY_RIGHT_BRACKET
			|| key == GLFW.GLFW_KEY_SEMICOLON
			|| key == GLFW.GLFW_KEY_APOSTROPHE
			|| key == GLFW.GLFW_KEY_GRAVE_ACCENT
			|| key == GLFW.GLFW_KEY_BACKSLASH
			|| key == GLFW.GLFW_KEY_COMMA
			|| key == GLFW.GLFW_KEY_PERIOD
			|| key == GLFW.GLFW_KEY_SLASH;
	}

	private void executeImmediateLine(String line) {
		if (line == null || line.isBlank()) return;

		HeadlessHost headless = new HeadlessHost();
		new QBasicInterpreter(headless).run(line);
		if (headless.hadError()) {
			pendingErrorCode = headless.getLastErrorCode();
			showFileError(headless.getLastErrorMessage());
			return;
		}

		immediate.appendOutput(headless.getOutput());
	}

	private int pendingErrorCode = 2;

	private void invokeMenuAction(String action) {
		mode = Mode.EDITOR;
		switch (action) {
			case "file.exit":
				if (modified) {
					showSaveChangesPrompt();
				} else {
					pendingExit = true;
				}
				return;
			case "file.save":
				saveFile();
				return;
			case "run.start":
				startRun();
				return;
			case "help.survival":
				showSurvivalGuide();
				return;
			default:
				statusMessage = "(action: " + action + ")";
		}
	}

	@Override
	public boolean charTyped(char cp, int mods) {
		if (consumingMenuKeystroke) {
			consumingMenuKeystroke = false;
			return true;
		}
		// RUNNING: feed INKEY$ or the INPUT buffer, depending on state.
		if (mode == Mode.RUNNING && host != null) {
			if (cp < 32 || cp == 127) return true;

			if (runState == RunState.WAITING_INPUT) {
				inputBuffer.append(cp);
				host.print(String.valueOf(cp));
			} else {
				if (keyEnqueuedInKeyPressed) {
					keyEnqueuedInKeyPressed = false;
				}
				else {
					host.enqueueKey(String.valueOf(cp));
				}
			}
			return true;
		}

		if (mode == Mode.RUN_OUTPUT) return true;
		if (mode == Mode.MENU)	 return true;
		if (mode == Mode.DIALOG) return true;

		if (mode == Mode.EDITOR && focus == Focus.IMMEDIATE) {
			immediate.insert(cp);
			return true;
		}
		if (altHeld) return true;
		return super.charTyped(cp, mods);
	}
	
	@Override
	protected boolean shouldDrawCursor() {
		return mode == Mode.EDITOR && focus == Focus.EDIT;
	}
	
	@Override
	protected boolean modeIsEditor() {
		return mode == Mode.EDITOR;
	}
	
	@Override
	public void onFileWriteResult(String path, boolean success, String message) {
		if (success) {
			statusMessage = "Written to " + path;
			modified = false;
		} else {
			showFileError(message);
		}
	}

	// Run pipeline
	private void startRun() {
		pendingSourceSnapshot = currentSource();

		// Fresh text screen for the program's output.
		setDisplayMode(new Screen0Text());

		host = new QBasicHostImpl(this);
		interpreter = new QBasicInterpreter(host);
		interpreter.start(pendingSourceSnapshot);

		inputBuffer.setLength(0);
		runState = RunState.RUNNING;
		mode = Mode.RUNNING;
	}

	private void restoreEditorAfterRun() {
		if (pendingSourceSnapshot == null) return;
		lines.clear();
		for (String l : pendingSourceSnapshot.split("\n", -1)) lines.add(new StringBuilder(l));
		cursorRow = 0; cursorCol = 0;
		scrollRow = 0; scrollCol = 0;
		pendingSourceSnapshot = null;

		// Restore to a fresh text screen so a subsequent run starts clean.
		setDisplayMode(new Screen0Text());
	}
	
	// Dialogs via TuiDialog
	private void showWelcomeDialog() {
		TuiDialog dlg = new TuiDialog()
				.addLine("")
				.addLine("Welcome to MS-DOS QBasic")
				.addLine("")
				.addLine("Copyright (C) Microsoft Corporation, 1987-1992.")
				.addLine("All rights reserved.")
				.addLine("")
				.addItem("Press Enter to see the Survival Guide", "help.survival")
				.addItem("Press ESC to clear this dialog box", "close");

		dlg.onAction(a -> {
			dismissOverlay(dlg);
			if (a.equals("help.survival")) showSurvivalGuide();
			else mode = Mode.EDITOR;
		});
		dlg.onCancel(() -> { dismissOverlay(dlg); mode = Mode.EDITOR; });
		showOverlay(dlg);
	}

	private void showSurvivalGuide() {
		TuiDialog dlg = new TuiDialog()
				.addLine("")
				.addLine("QBasic Survival Guide")
				.addLine("")
				.addLine("F5 runs your program.")
				.addLine("F2 saves to disk.")
				.addLine("ALT opens the menu bar.")
				.addLine("")
				.addItem("Press ESC to close", "close");

		dlg.onAction(a -> { dismissOverlay(dlg); mode = Mode.EDITOR; });
		dlg.onCancel(() -> { dismissOverlay(dlg); mode = Mode.EDITOR; });
		showOverlay(dlg);
		mode = Mode.DIALOG;
	}

	private void showFileError(String message) {
		TuiDialog dlg = new TuiDialog()
				.addLine("")
				.addLine(message == null || message.isEmpty() ? "Invalid syntax" : message)
				.addLine("")
				.addItem("OK",	 "err.ok")
				.addItem("Help", "err.help");

		dlg.onAction(a -> {
			dismissOverlay(dlg);
			mode = Mode.EDITOR;
			if (a.equals("err.help")) {
				TuiDialog help = new TuiDialog()
						.addLine("")
						.addLine("ERR code: " + pendingErrorCode)
						.addLine("")
						.addLine("Press ESC to close")
						.addLine("")
						.addItem("OK", "close");
				help.onAction(x -> { dismissOverlay(help); mode = Mode.EDITOR; });
				help.onCancel(() -> { dismissOverlay(help); mode = Mode.EDITOR; });
				showOverlay(help);
			}
		});
		dlg.onCancel(() -> { dismissOverlay(dlg); mode = Mode.EDITOR; });
		showOverlay(dlg);
		mode = Mode.DIALOG;
	}
	private void showSaveChangesPrompt() {
		TuiDialog dlg = new TuiDialog()
				.addLine("")
				.addLine("Loaded file is not saved. Save it now?")
				.addLine("")
				.addItem("Yes",    "save.yes")
				.addItem("No",	   "save.no")
				.addItem("Cancel", "save.cancel")
				.addItem("Help",   "save.help")
				.horizontal();

		dlg.onAction(a -> {
			dismissOverlay(dlg);
			switch (a) {
				case "save.yes":
					saveFile();
					pendingExit = true;
					break;
				case "save.no":
					pendingExit = true;
					break;
				case "save.cancel":
					mode = Mode.EDITOR;
					break;
				case "save.help":
					showSaveHelpDialog();
					break;
			}
		});
		dlg.onCancel(() -> { dismissOverlay(dlg); mode = Mode.EDITOR; });

		showOverlay(dlg);
		mode = Mode.DIALOG;
	}

	private void showSaveHelpDialog() {
		TuiDialog help = new TuiDialog()
				.addLine("")
				.addLine("Save changes?")
				.addLine("")
				.addLine("Yes	  Save and exit")
				.addLine("No	  Exit without saving")
				.addLine("Cancel  Return to the editor")
				.addLine("")
				.addItem("OK", "help.ok");

		help.onAction(a -> {
			dismissOverlay(help);
			showSaveChangesPrompt();
		});
		help.onCancel(() -> {
			dismissOverlay(help);
			showSaveChangesPrompt();
		});

		showOverlay(help);
	}
	@Override
	public String getTitle() { return "QBASIC - " + filePath; }
}
