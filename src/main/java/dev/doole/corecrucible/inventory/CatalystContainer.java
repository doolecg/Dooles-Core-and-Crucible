package dev.doole.corecrucible.inventory;

import net.minecraft.world.SimpleContainer;

/** One-slot container that re-runs the smithing result when it changes (26.x SimpleContainer has no listeners). */
public class CatalystContainer extends SimpleContainer {
    private final Runnable onChanged;

    public CatalystContainer(Runnable onChanged) {
        super(1);
        this.onChanged = onChanged;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        onChanged.run();
    }
}
