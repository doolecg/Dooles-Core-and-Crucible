package dev.doole.corecrucible.platform.fabric;

//? fabric {

import dev.doole.corecrucible.platform.MultiLoaderUtil;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

// Fabric's answers to the loader questions in MultiLoaderUtil.
public class FabricLoaderUtil implements MultiLoaderUtil {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

}
//?}
