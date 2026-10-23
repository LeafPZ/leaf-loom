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
