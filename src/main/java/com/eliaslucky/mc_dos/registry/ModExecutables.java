package com.eliaslucky.mc_dos.registry;

import com.eliaslucky.mc_dos.api.exec.ExecutableRegistry;
import com.eliaslucky.mc_dos.blocks.computer.kernel.dos.drivers.DosMccmdDriver;

public final class ModExecutables {
    private ModExecutables() {}

    /** Called once from mod setup. Idempotent. */
    public static void register() {

        // ── MS-DOS ───────────────────────────────────────────────────────
        ExecutableRegistry.register("dos", "QBASIC.EXE",
                "MZ\u0090\u0000\u0003\u0000\u0000\u0000",
                "MZ\u0090\u0000\u0003\u0000\u0000\u0000Microsoft QuickBASIC\n",
                DosExecutables::runQBasic);

        ExecutableRegistry.register("dos", "EDIT.COM",
                null,
                "\u00B4MS-DOS Editor\n",
                DosExecutables::runEdit);

        ExecutableRegistry.register("dos", "GWBASIC.EXE",
                "MZ\u0090\u0000\u0003\u0000\u0000\u0000",
                "MZ\u0090\u0000\u0003\u0000\u0000\u0000GW-BASIC 3.22\n",
                DosExecutables::runGwBasic);

        // ── POSIX (Linux and UNIX share these) ───────────────────────────
        ExecutableRegistry.register("posix", "/bin/sh",
                "#!",
                "#!/bin/sh\n",
                (c, a, f) -> PosixExecutables.runSh(c, a));

        ExecutableRegistry.register("posix", "/bin/bash",
                "\u007fELF",
                "\u007fELF /bin/bash\n",
                (c, a, f) -> PosixExecutables.runBash(c, a));

        ExecutableRegistry.register("posix", "/bin/ls",
                "\u007fELF",
                "\u007fELF /bin/ls\n",
                PosixExecutables::runLs);

        ExecutableRegistry.register("posix", "/bin/cat",
                "\u007fELF",
                "\u007fELF /bin/cat\n",
                PosixExecutables::runCat);
        // ... whatever else
    }
}
