package com.eliaslucky.mc_dos.client.apps;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.qbasic.QBasicApplication;

import java.util.*;

public final class TerminalApplicationRegistry {
    public interface AppFactory {
        TerminalApplication create(ComputerTerminalScreen screen, String[] args, String initialContent);
    }

    private static final Map<String, AppFactory> REGISTRY = new HashMap<>();

    public static void register(String name, AppFactory f) {
        REGISTRY.put(name.toUpperCase(Locale.ROOT), f);
    }

    public static AppFactory get(String name) {
        return REGISTRY.get(name.toUpperCase(Locale.ROOT));
    }

    static {
        register("QBASIC",     QBasicApplication::new);
        register("QBASIC.EXE", QBasicApplication::new);
        //register("GWBASIC",    GWBasicApplication::new);
        // later: register("VI", ViApplication::new); etc.
    }
}
