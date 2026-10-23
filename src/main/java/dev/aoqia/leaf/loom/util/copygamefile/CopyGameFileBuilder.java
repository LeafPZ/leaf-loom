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
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

@SuppressWarnings("UnusedReturnValue")
public class CopyGameFileBuilder {
    private static final Duration ONE_DAY = Duration.ofDays(1);

    private final Path path;
    private String expectedHash = null;
    private boolean forced = false;
    private boolean fallback = false;
    private Duration maxAge = Duration.ZERO;
    private CopyGameFileProgressListener progressListener = CopyGameFileProgressListener.NONE;
    private int maxRetries = 3;

    private CopyGameFileBuilder(Path path) {
        this.path = path;
    }

    static CopyGameFileBuilder create(Path path) {
        return new CopyGameFileBuilder(path);
    }

    public CopyGameFileBuilder sha1(String sha1) {
        this.expectedHash = "sha1:" + sha1;
        return this;
    }

    public CopyGameFileBuilder forced() {
        forced = true;
        return this;
    }

    public CopyGameFileBuilder fallback(boolean force) {
        this.fallback = force;
        return this;
    }

    public CopyGameFileBuilder progress(CopyGameFileProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    public CopyGameFileBuilder maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public CopyGameFileBuilder defaultCache() {
        return maxAge(ONE_DAY);
    }

    public CopyGameFileBuilder maxAge(Duration duration) {
        this.maxAge = duration;
        return this;
    }

    public void copyGameFileFromPathAsync(Path path, CopyGameFileExecutor executor) {
        executor.runAsync(() -> copyGameFileFromPath(path));
    }

    public void copyGameFileFromPath(Path path) throws IOException {
        withRetries((copyGameFile) -> {
            copyGameFile.copyGameFile(path);
            return null;
        });
    }

    private <T> T withRetries(CopyGameFileFn<T> supplier) throws IOException {
        for (int i = 1; i <= maxRetries; i++) {
            try {
                return supplier.get(build(i));
            } catch (IOException e) {
                if (i == maxRetries) {
                    throw new IOException(
                        String.format(Locale.ENGLISH, "Failed copy after %d attempts", maxRetries), e
                    );
                }
            }
        }

        throw new IllegalStateException();
    }

    private CopyGameFile build(int attempt) {
        return new CopyGameFile(
            this.path, this.expectedHash, this.maxAge, this.forced, this.fallback, attempt, this.progressListener
        );
    }

    @FunctionalInterface
    private interface CopyGameFileFn<T> {
        T get(CopyGameFile copyGameFile) throws IOException;
    }
}
