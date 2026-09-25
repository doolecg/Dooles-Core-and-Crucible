package dev.doole.corecrucible.platform;

//$ loader_util_import
import dev.doole.corecrucible.platform.fabric.FabricLoaderUtil;
import java.nio.file.Path;

// Small set of questions that are answered differently on Fabric and NeoForge (is this other mod loaded, where do
// config files go). Stonecutter swaps in the right loader's implementation at build time.
public interface MultiLoaderUtil {
    MultiLoaderUtil INSTANCE = /*$ loader_util_inst*/ new FabricLoaderUtil();

    boolean isModLoaded(String modId);

    /** The game's config folder, where the balance config lives. */
    Path configDir();
}
