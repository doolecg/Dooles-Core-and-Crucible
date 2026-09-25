package dev.doole.corecrucible.platform.neoforge;

//? neoforge {

/*import dev.doole.corecrucible.platform.MultiLoaderUtil;
import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

// NeoForge's answers to the loader questions in MultiLoaderUtil.
public class NeoforgeLoaderUtil implements MultiLoaderUtil {

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
*///?}
