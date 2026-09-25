package dev.doole.corecrucible.platform;

//$ version_util_import
import dev.doole.corecrucible.platform.version.Util26_3;

// Extension point for code that differs between 1.21.1, 26.2 and 26.3. Stonecutter picks the matching Util*
// class for whichever version is being built. No version currently needs a method here, so it's empty for now.
public interface MultiVersionUtil {
    MultiVersionUtil INSTANCE = /*$ version_util_inst*/ new Util26_3();
}
