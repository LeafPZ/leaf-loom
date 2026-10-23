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
