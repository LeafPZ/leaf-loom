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

package dev.aoqia.leaf.loom.util.copygamefile;

import java.util.Objects;
import java.util.function.Function;

import org.gradle.internal.logging.progress.ProgressLogger;
import org.jetbrains.annotations.Nullable;

public class GradleCopyGameFileProgressListener implements CopyGameFileProgressListener {
    private final String name;
    private final Function<String, ProgressLogger> progressLoggerFactory;

    @Nullable private ProgressLogger progressLogger;

    public GradleCopyGameFileProgressListener(String name, Function<String, ProgressLogger> progressLoggerFactory) {
        this.name = name;
        this.progressLoggerFactory = progressLoggerFactory;
    }

    @Override
    public void onStart() {
        progressLogger = progressLoggerFactory.apply(this.name);
    }

    @Override
    public void onProgress(long bytesTransferred, long contentLength) {
        Objects.requireNonNull(progressLogger);
        progressLogger.progress(
            "Downloading %s - %s / %s".formatted(name, humanBytes(bytesTransferred), humanBytes(contentLength))
        );
    }

    @Override
    public void onEnd() {
        if (progressLogger != null) {
            progressLogger.completed();
            progressLogger = null;
        }
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
