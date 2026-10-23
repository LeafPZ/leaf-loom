package dev.aoqia.leaf.loom.util.copygamefile;

import java.nio.file.Path;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import dev.aoqia.leaf.loom.LoomGradleExtension;

/**
 * Can be used to create a {@link CopyGameFileBuilder} with the correct settings
 * for the project within a task.
 */
public abstract class CopyGameFileFactory {
    @Inject
    public CopyGameFileFactory() {
        getIsManualRefreshDependencies().set(LoomGradleExtension.get(getProject()).refreshDeps());
    }

    @Input
    protected abstract Property<Boolean> getIsManualRefreshDependencies();

    @Inject
    public abstract Project getProject();

    // Matches the logic in LoomGradleExtensionImpl
    public CopyGameFileBuilder copyGameFile(String path) {
        CopyGameFileBuilder builder = CopyGameFile.create(Path.of(path));

        if (getIsManualRefreshDependencies().get()) {
            builder.forced();
        }

        return builder;
    }
}
