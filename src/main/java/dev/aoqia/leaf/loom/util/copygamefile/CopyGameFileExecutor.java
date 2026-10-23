package dev.aoqia.leaf.loom.util.copygamefile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CopyGameFileExecutor implements AutoCloseable {
    private final ExecutorService executorService;
    private final List<IOException> copyExceptions = Collections.synchronizedList(new ArrayList<>());

    public CopyGameFileExecutor(int threads) {
        executorService = Executors.newFixedThreadPool(threads);
    }

    void runAsync(CopyGameFileRunner copyGameFileRunner) {
        if (!copyExceptions.isEmpty()) {
            return;
        }

        executorService.execute(() -> {
            try {
                copyGameFileRunner.run();
            } catch (IOException e) {
                executorService.shutdownNow();
                copyExceptions.add(e);
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        executorService.shutdown();

        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!copyExceptions.isEmpty()) {
            IOException copyException = new IOException("Failed to copy game files to cache");

            for (IOException suppressed : copyExceptions) {
                copyException.addSuppressed(suppressed);
            }

            throw copyException;
        }
    }

    @FunctionalInterface
    public interface CopyGameFileRunner {
        void run() throws IOException;
    }
}
