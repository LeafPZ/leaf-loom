package dev.aoqia.leaf.loom.task;

import java.io.IOException;

import org.apache.commons.io.FileUtils;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.work.DisableCachingByDefault;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.util.Constants;

@DisableCachingByDefault(because = "Produces no cacheable outputs")
@UntrackedTask(because = "Produces no cacheable inputs")
public abstract class ClearLoomCacheTask extends DefaultTask {
    public ClearLoomCacheTask() {
        setGroup(Constants.TaskGroup.LEAF);
    }

    @Internal
    protected LoomGradleExtension getExtension() {
        return LoomGradleExtension.get(getProject());
    }

    @TaskAction
    public void run() {
        try {
            FileUtils.deleteDirectory(getExtension().getFiles().getUserCache());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete leaf-loom gradle cache directory.");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                "Expected directory but found file when trying to delete leaf-loom gradle cache."
            );
        }
    }
}
