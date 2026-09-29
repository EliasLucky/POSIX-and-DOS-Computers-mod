package com.eliaslucky.mc_dos.client.tui;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A registry of named {@link TuiTheme}s.
 *
 * <p>Built-in themes are registered at class load. Addons register
 * their own from mod setup:
 *
 * <pre>{@code
 * TuiThemes.register(
 *	   ResourceLocation.fromNamespaceAndPath("myaddon", "c64"),
 *	   myC64Theme);
 * }</pre>
 *
 * <p>Lookup accepts either a full {@code ResourceLocation} or a
 * shorthand {@code String} — {@code "blockos:norton"} or just
 * {@code "norton"} for the default namespace.
 *
 * @since 1.5
 */
public final class TuiThemes {
	private TuiThemes() {}

	/** Default namespace for the mod's own themes. */
	public static final String DEFAULT_NAMESPACE = "mc_dos";

	private static final Map<ResourceLocation, TuiTheme> REGISTRY = new LinkedHashMap<>();

	// Built-in themes

	/** QBasic's blue-on-blue editor. Matches the QBASIC IDE look. */
	public static final TuiTheme QBASIC = new TuiTheme(
			TuiPalette.BLUE, // screenBg
			TuiPalette.LIGHT_GRAY, // screenFg
			TuiPalette.LIGHT_GRAY, // titleBg
			TuiPalette.BLACK, // titleFg
			TuiPalette.BLUE, //highlgihtBg
			TuiPalette.WHITE, //highlightFg
			TuiPalette.YELLOW, //highlightMn
			TuiPalette.LIGHT_GRAY, //frameBg
			TuiPalette.BLACK, //border
			TuiPalette.LIGHT_GRAY, // statusBg
			TuiPalette.BLACK, // statusFg
			TuiPalette.WHITE,
			TuiPalette.DARK_GRAY,
			TuiPalette.YELLOW, TuiPalette.LIGHT_RED, TuiPalette.LIGHT_GREEN);

	/** Norton Commander's cyan-on-blue file panel. */
	public static final TuiTheme NORTON_COMMANDER = new TuiTheme(
			TuiPalette.BLUE,		 TuiPalette.LIGHT_GRAY,
			TuiPalette.CYAN,		 TuiPalette.BLACK,
			TuiPalette.CYAN,		 TuiPalette.BLACK,		 TuiPalette.RED,
			TuiPalette.BLUE,		 TuiPalette.LIGHT_GRAY,
			TuiPalette.CYAN,		 TuiPalette.BLACK,
			TuiPalette.WHITE,
			TuiPalette.DARK_GRAY,
			TuiPalette.YELLOW, TuiPalette.LIGHT_RED, TuiPalette.LIGHT_GREEN);

	/** MSD color palette */
	public static final TuiTheme MSD = new TuiTheme(
			TuiPalette.CYAN,
			TuiPalette.BLACK,
			TuiPalette.LIGHT_GRAY,
			TuiPalette.BLACK,
			TuiPalette.BLACK,
			TuiPalette.WHITE,
			TuiPalette.LIGHT_RED,
			TuiPalette.LIGHT_GRAY,
			TuiPalette.BLACK,
			TuiPalette.LIGHT_GRAY,
			TuiPalette.BLACK,
			TuiPalette.WHITE,
			TuiPalette.DARK_GRAY,
			TuiPalette.YELLOW,
			TuiPalette.LIGHT_RED,
			TuiPalette.LIGHT_GREEN
			);

	/** IBM AT BIOS SETUP: black text on light gray, monochrome. */
	public static final TuiTheme IBM_AT_SETUP = new TuiTheme(
			TuiPalette.LIGHT_GRAY,	 TuiPalette.BLACK,
			TuiPalette.BLACK,		 TuiPalette.LIGHT_GRAY,
			TuiPalette.BLACK,		 TuiPalette.WHITE,		 TuiPalette.YELLOW,
			TuiPalette.LIGHT_GRAY,	 TuiPalette.BLACK,
			TuiPalette.BLACK,		 TuiPalette.LIGHT_GRAY,
			TuiPalette.BLACK,
			TuiPalette.DARK_GRAY,
			TuiPalette.RED, TuiPalette.RED, TuiPalette.GREEN);

	public static final TuiTheme AWARD_SETUP = new TuiTheme(
			TuiPalette.BLUE,		  // screenBg
			TuiPalette.LIGHT_GRAY,	  // screenFg
			TuiPalette.BLUE,		  // titleBg
			TuiPalette.YELLOW,		  // titleFg
			TuiPalette.LIGHT_GRAY,	  // highlightBg
			TuiPalette.BLACK,		  // highlightFg
			TuiPalette.RED,			  // highlightMn
			TuiPalette.LIGHT_GRAY,	  // frameBg
			TuiPalette.LIGHT_GRAY,	  // border
			TuiPalette.LIGHT_GRAY,	  // statusBg
			TuiPalette.BLACK,		  // statusFg
			TuiPalette.YELLOW,		  // value
			TuiPalette.DARK_GRAY,	  // disabled
			TuiPalette.LIGHT_RED,	  // warning
			TuiPalette.LIGHT_RED,	  // error
			TuiPalette.LIGHT_GREEN	  // success
	);

	/** Turbo Pascal IDE: blue editor, yellow text, gray chrome. */
	public static final TuiTheme TURBO_PASCAL = new TuiTheme(
			TuiPalette.BLUE,		 TuiPalette.YELLOW,
			TuiPalette.LIGHT_GRAY,	 TuiPalette.BLACK,
			TuiPalette.LIGHT_GRAY,	 TuiPalette.BLACK,		 TuiPalette.RED,
			TuiPalette.BLUE,		 TuiPalette.LIGHT_GRAY,
			TuiPalette.LIGHT_GRAY,	 TuiPalette.BLACK,
			TuiPalette.WHITE,
			TuiPalette.DARK_GRAY,
			TuiPalette.YELLOW, TuiPalette.LIGHT_RED, TuiPalette.LIGHT_GREEN);

	static {
		register(id("qbasic"),		  QBASIC);
		register(id("norton"),		  NORTON_COMMANDER);
		register(id("msd"),               MSD);
		register(id("ibm_at_setup"),	  IBM_AT_SETUP);
		register(id("award_setup"),	  AWARD_SETUP);
		register(id("turbo_pascal"),	  TURBO_PASCAL);
	}

	// Registration

	/**
	 * Register a theme. Called by addons from mod setup.
	 *
	 * @param key	the theme's identifier, usually {@code <modid>:<name>}
	 * @param theme the theme
	 */
	public static void register(ResourceLocation key, TuiTheme theme) {
		REGISTRY.put(key, theme);
	}

	/**
	 * Look up a theme by full or shorthand identifier.
	 *
	 * @param name either {@code "namespace:path"} or just {@code "path"},
	 *			   in which case the default namespace is assumed
	 * @return the theme, or {@code null} if none is registered
	 */
	public static TuiTheme get(String name) {
		if (name == null) return null;
		if (!name.contains(":")) {
			name = DEFAULT_NAMESPACE + ":" + name;
		}
		return REGISTRY.get(ResourceLocation.parse(name));
	}

	/** @return an unmodifiable view of all registered themes. */
	public static Map<ResourceLocation, TuiTheme> all() {
		return Map.copyOf(REGISTRY);
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(DEFAULT_NAMESPACE, path);
	}
}
