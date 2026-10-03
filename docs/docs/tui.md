# TUI Application Programming

## 1.0 Purpose

This document describes TUI API of the "POSIX and DOS Computers" mod
modification. It explains the widget model, the container that
dispatches events, the theme system, and the procedure for writing a
terminal application.

## 2.0 Overview

A terminal application is a full-screen program that runs inside a
computer block. The application draws characters on an 80-column grid
and receives keyboard input. The framework provides:

- A character-grid widget model.
- A container that dispatches render and input events.
- A theme system for colors.
- A launch protocol for the server to request that the client open an
  application.

Applications are registered in `TerminalApplicationRegistry`. The
registry is populated on the client during `ClientSetup`.

## 3.0 The TerminalApplication Class

Class name: `com.eliaslucky.mc_dos.client.apps.TerminalApplication`

Methods that subclasses override:

- `render (GuiGraphics g, int mouseX, int mouseY, float partialTick)
  returns void` - Draw the application. Required.

- `keyPressed (int keyCode, int scanCode, int modifiers)
  returns boolean` - Handle a key press. Return true to consume.

- `charTyped (char cp, int mods) returns boolean` - Handle a typed
  character. Return true to consume.

- `keyReleased (int keyCode, int scanCode, int modifiers)
  returns boolean` - Handle a key release.

- `mouseClicked (double x, double y, int button) returns boolean` -
  Handle a mouse click.

- `mouseScrolled (double x, double y, double delta) returns boolean`
  - Handle a scroll event.

- `onClose () returns void` - Called when the application is closed.
  Release resources here.

- `getTitle () returns String` - The window title.

Fields and methods provided by the base class:

- `CELL_W (int, constant, value 8)` - Width of one character cell in
  pixels.
- `CELL_H (int, constant, value 16)` - Height of one character cell.
- `appWidth (int)` - Current application width in pixels.
- `appHeight (int)` - Current application height in pixels.
- `cols () returns int` - Number of character columns.
- `rows () returns int` - Number of character rows.
- `theme () returns TuiTheme` - The active theme.
- `setTheme (TuiTheme theme) returns void` - Installs a new theme.
- `drawDos (GuiGraphics g, String text, int x, int y, int color)
  returns void` - Draws text using the terminal's DOS font.

## 4.0 The Widget Model

Interface name: `com.eliaslucky.mc_dos.client.tui.TuiWidget`

A widget occupies a rectangular region of the character grid and
knows how to draw itself and handle input.

Methods:

- `row () returns int` - Top row in character cells.
- `col () returns int` - Left column in character cells.
- `width () returns int` - Width in character cells.
- `height () returns int` - Height in character cells.
- `render (GuiGraphics g, TerminalApplication app) returns void` -
  Draw the widget.
- `keyPressed (int keyCode, int scanCode, int modifiers)
  returns boolean` - Handle a key press. Default returns false.
- `charTyped (char cp, int mods) returns boolean` - Handle a typed
  character. Default returns false.
- `mouseClicked (double x, double y, int button) returns boolean` -
  Handle a mouse click. Default returns false.
- `focused () returns boolean` - Whether the widget holds focus.
  Default returns false.
- `setFocused (boolean f) returns void` - Set focus state. Default is
  a no-op.

## 5.0 The TuiScreen Container

Class name: `com.eliaslucky.mc_dos.client.tui.TuiScreen`

The screen is a container for widgets. It routes events to the focused
widget first, then to each other widget in insertion order.

Methods:

- `add (TuiWidget w) returns void` - Append a widget.
- `remove (TuiWidget w) returns void` - Remove a widget. If it held
  focus, focus is cleared.
- `clear () returns void` - Remove all widgets.
- `setFocus (TuiWidget w) returns void` - Give keyboard focus to a
  widget. Pass `null` to clear.
- `getFocus () returns TuiWidget` - The focused widget, or `null`.
- `widgets () returns List<TuiWidget>` - Unmodifiable view.
- `render (GuiGraphics g, TerminalApplication app) returns void` -
  Render all widgets in order.
- `keyPressed (int keyCode, int scanCode, int modifiers)
  returns boolean` - Dispatch a key press.
- `charTyped (char cp, int mods) returns boolean` - Dispatch a typed
  character.
- `mouseClicked (double x, double y, int button) returns boolean` -
  Dispatch a mouse click in reverse insertion order.

## 6.0 Available Widgets

### 6.1 TuiBox

Class name: `com.eliaslucky.mc_dos.client.tui.TuiBox`

A framed rectangle with an optional title.

Constructor:

- `TuiBox (int row, int col, int width, int height, Style style)`

Style values:

- `TuiBox.Style.SINGLE` - Single-line border, `┌─┐│└─┘`.
- `TuiBox.Style.DOUBLE` - Double-line border, `╔═╗║╚═╝`.

Builder methods:

- `titled (String title) returns TuiBox` - Set the title. The title
  is spliced into the top border, not drawn on top of it.
- `border (int color) returns TuiBox` - Set the border color.
- `fill (int color) returns TuiBox` - Set the interior fill color.
- `transparent () returns TuiBox` - Leave the interior untouched.
- `themed (TuiTheme theme) returns TuiBox` - Set border and fill from
  a theme.

### 6.2 TuiLabel

Class name: `com.eliaslucky.mc_dos.client.tui.TuiLabel`

A single line of static text. Never consumes input.

Constructors:

- `TuiLabel (int row, int col, String text)` - Color from the theme.
- `TuiLabel (int row, int col, String text, int color)` - Explicit
  color.

Methods:

- `setText (String text) returns void`
- `setColor (int color) returns void`

### 6.3 TuiMenu

Class name: `com.eliaslucky.mc_dos.client.tui.TuiMenu`

A menu bar with dropdown menus.

Constructor:

- `TuiMenu (int row, List<Menu> menus)`

Menu and item records:

    public record Item(String label, char mnemonic, String action,
                       String footerHelp) {}
    public record Menu(String label, char mnemonic, List<Item> items) {}

Methods:

- `open () returns void` - Open the first menu.
- `close () returns void` - Close the dropdown.
- `openByMnemonic (char m) returns boolean` - Open a specific menu.
- `isOpen () returns boolean`
- `footerHelp () returns String`
- `onAction (Consumer<String> c) returns void`
- `render (GuiGraphics g, TerminalApplication app) returns void` -
  Draws only the top strip.
- `renderOverlay (GuiGraphics g, TerminalApplication app) returns
  void` - Draws the dropdown. Must be called after every other widget
  on the screen so that the dropdown draws on top.
- `keyPressed (int keyCode, int scanCode, int modifiers)
  returns boolean`

### 6.4 TuiDialog

Class name: `com.eliaslucky.mc_dos.client.tui.TuiDialog`

A modal dialog box with optional body text and selectable items.

Builder methods:

- `addLine (String line) returns TuiDialog` - Append a body line.
- `addItem (String label, String action) returns TuiDialog` - Append
  a selectable item.
- `horizontal () returns TuiDialog` - Lay out items in a single row.
- `vertical () returns TuiDialog` - Lay out items in a column. This
  is the default.
- `onAction (Consumer<String> c) returns TuiDialog`
- `onCancel (Runnable r) returns TuiDialog`
- `fill (int color) returns TuiDialog`
- `border (int color) returns TuiDialog`
- `text (int color) returns TuiDialog`
- `highlight (int bg, int fg) returns TuiDialog`

A dialog is added to a `TuiScreen` and given focus:

    TuiDialog dlg = new TuiDialog()
            .addLine("Are you sure?")
            .addItem("Yes", "yes")
            .addItem("No",  "no");

    dlg.onAction(a -> { overlay.remove(dlg); /* handle a */ });
    dlg.onCancel(() -> { overlay.remove(dlg); });

    overlay.add(dlg);
    overlay.setFocus(dlg);

### 6.5 TuiList

Class name: `com.eliaslucky.mc_dos.client.tui.TuiList`

A vertical list of strings with keyboard selection.

Constructor:

- `TuiList (int row, int col, int width, int height)`

Methods:

- `setItems (List<String> items) returns void`
- `selectedIndex () returns int`
- `selectedItem () returns String`
- `setSelected (int index) returns void`
- `onActivate (Consumer<Integer> c) returns void`

### 6.6 TuiKeyValueTable

Class name: `com.eliaslucky.mc_dos.client.tui.TuiKeyValueTable`

A two-column table of key-value rows. Used by BIOS SETUP screens.

Constructor:

- `TuiKeyValueTable (int row, int col, int width, int height)`

Row record:

    public record Row(String key, String value, boolean editable,
                      String action) {}

Methods:

- `setRows (List<Row> rows) returns void`
- `setValue (int index, String value) returns void`
- `selectedIndex () returns int`
- `setSelected (int index) returns void`
- `onAction (Consumer<String> c) returns void`

### 6.7 TuiProgressBar

Class name: `com.eliaslucky.mc_dos.client.tui.TuiProgressBar`

A horizontal progress bar.

Constructor:

- `TuiProgressBar (int row, int col, int width)`

Methods:

- `setProgress (double p) returns void` - Fraction between 0.0 and
  1.0.
- `progress () returns double`

## 7.0 Colors and Themes

### 7.1 TuiPalette

Class name: `com.eliaslucky.mc_dos.client.tui.TuiPalette`

Provides the sixteen base EGA colors as named constants, plus role
defaults. Widgets that have no theme fall back to these.

Base colors:

`BLACK`, `BLUE`, `GREEN`, `CYAN`, `RED`, `MAGENTA`, `BROWN`,
`LIGHT_GRAY`, `DARK_GRAY`, `LIGHT_BLUE`, `LIGHT_GREEN`, `LIGHT_CYAN`,
`LIGHT_RED`, `LIGHT_MAGENTA`, `YELLOW`, `WHITE`.

### 7.2 TuiTheme

Record name: `com.eliaslucky.mc_dos.client.tui.TuiTheme`

Maps semantic UI roles to colors. A theme has twenty-one components:

Core roles:

- `screenBg (int)` - Background behind everything.
- `screenFg (int)` - Default body text.
- `titleBg (int)` - Menu bar and title bar background.
- `titleFg (int)` - Menu bar and title bar text.
- `highlightBg (int)` - Selected row background.
- `highlightFg (int)` - Selected row text.
- `highlightMn (int)` - Selected row mnemonic.
- `frameBg (int)` - Dialog and box interior.
- `border (int)` - Box and dialog border.
- `statusBg (int)` - Status bar background.
- `statusFg (int)` - Status bar text.
- `value (int)` - Editable or emphasized values.
- `disabled (int)` - Greyed-out text.
- `warning (int)`, `error (int)`, `success (int)` - Semantic colors.

Extended roles:

- `progressFill (int)`, `progressEmpty (int)` - Progress bar.
- `scrollTrack (int)`, `scrollThumb (int)`, `scrollArrow (int)` -
  Scrollbar.

A sixteen-argument convenience constructor derives the extended roles
from the core ones.

### 7.3 TuiThemes

Class name: `com.eliaslucky.mc_dos.client.tui.TuiThemes`

Registry of named themes.

Built-in themes:

- `QBASIC` - Blue screen, light gray chrome.
- `NORTON_COMMANDER` - Blue screen, cyan chrome.
- `IBM_AT_SETUP` - Light gray screen, black text.
- `AWARD_SETUP` - Blue screen, yellow title and values.
- `TURBO_PASCAL` - Blue screen, yellow text.

Methods:

- `register (ResourceLocation key, TuiTheme theme) returns void`
- `get (String name) returns TuiTheme`
- `get (ResourceLocation key) returns TuiTheme`
- `exists (String name) returns boolean`
- `all () returns Map<ResourceLocation, TuiTheme>`

Addons register their own themes during client setup:

    TuiThemes.register(
            ResourceLocation.fromNamespaceAndPath("myaddon", "amber"),
            new TuiTheme(
                    TuiPalette.BLACK, TuiPalette.BROWN,
                    TuiPalette.BLACK, TuiPalette.YELLOW,
                    TuiPalette.BROWN, TuiPalette.BLACK, TuiPalette.YELLOW,
                    TuiPalette.BLACK, TuiPalette.BROWN,
                    TuiPalette.BROWN, TuiPalette.BLACK,
                    TuiPalette.YELLOW, TuiPalette.BROWN,
                    TuiPalette.LIGHT_RED, TuiPalette.LIGHT_RED,
                    TuiPalette.YELLOW));

## 8.0 The APP_LAUNCH Protocol

Applications are launched by a string protocol. A server-side
executable runner returns a string of the form:

    APP_LAUNCH:NAME:ARGS:CONTENT

Fields:

- `NAME (String)` - The name under which the application is
  registered in `TerminalApplicationRegistry`. Uppercase by
  convention.
- `ARGS (String)` - Arguments. Passed to the factory as a
  single-element `String[]`.
- `CONTENT (String)` - Initial content. For editors, the file body.
  May contain colons.

The client splits the string on the first three colons only, so
`CONTENT` may contain colons without breaking the protocol.

When the terminal screen receives an output string beginning with
`APP_LAUNCH:`, it looks up the name in the registry. If a factory is
found, the factory constructs the application and the terminal hands
control to it. If no factory is found, the terminal prints
`Cannot launch app: <NAME>` and stays in line mode.

## 9.0 Writing an Application

Step 1. Create a class that extends `TerminalApplication`.

Step 2. Implement `render` and `keyPressed`. Optionally override
`charTyped` and the mouse methods.

Step 3. Implement `getTitle`.

Step 4. Optionally override `onClose` to release resources.

Step 5. Register the application in `ClientSetup` during client
setup:

    TerminalApplicationRegistry.register("CALC",
            CalcApplication::new);

Step 6. If the application should be launchable from a shell, register
an executable that returns the launch string. See
`ExecutableRegistry`.

Step 7. If the application saves files, implement `FileAwareApp` so
that it receives write results.

Example:

    public class CalcApplication extends TerminalApplication {

        private final StringBuilder display = new StringBuilder();

        public CalcApplication(ComputerTerminalScreen screen,
                                String[] args, String content) {
            super(screen);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY,
                            float partialTick) {
            g.fill(0, 0, appWidth, appHeight, theme().screenBg());
            drawDos(g, " Calculator ", 0, 0, theme().titleFg());
            drawDos(g, display.toString(), 0, CELL_H, theme().screenFg());
        }

        @Override
        public boolean keyPressed(int key, int scan, int mods) {
            if (key == GLFW.GLFW_KEY_ESCAPE) return false;
            return true;
        }

        @Override public boolean charTyped(char cp, int mods) {
            display.append(cp);
            return true;
        }

        @Override public String getTitle() { return "Calculator"; }
    }

## 10.0 Reference

### 10.1 Class Index

- `TerminalApplication` - Base class for applications.
- `TerminalApplicationRegistry` - Client-side app registry.
- `FileAwareApp` - Interface for apps that save files.
- `TuiWidget` - Widget contract.
- `TuiScreen` - Widget container.
- `TuiBox`, `TuiLabel`, `TuiMenu`, `TuiDialog`, `TuiList`,
  `TuiKeyValueTable`, `TuiProgressBar` - Widgets.
- `TuiPalette` - Base colors.
- `TuiTheme` - Role-to-color mapping.
- `TuiThemes` - Theme registry.

### 10.2 Character Cell Dimensions

- Cell width: 8 pixels.
- Cell height: 16 pixels.
- Default grid: 80 columns by 25 rows.

The grid dimensions follow the window size. Applications must
recompute layout on every render or on `onResize`.
