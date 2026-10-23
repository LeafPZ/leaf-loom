/*
 * Copyright (C) 2026 aoqia
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 *
 * This license and file are additionally subjected to the exceptions described in LICENSE_EXCEPTION.md.
 *
 * The exceptions permit independent software to use this project as a dependency under terms of its own choosing, but
 * does not permit the copying or incorporation of covered source code into another work outside the terms of the AGPL.
 */

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
