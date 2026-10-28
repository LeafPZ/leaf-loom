// spotless:off

/*
 * Copyright 2026 aoqia
 *
 * This file was originally released under LICENSE_UPSTREAM (see below for the original header) and has been relicensed
 * to GNU's AGPLv3 license, with additional terms in the LICENSE_EXCEPTION.md file.
 *
 * All modifications made to this file after the original version were created separate to the original copyright.
 * These modifications are provided under the AGPLv3 license (including exception terms). The portions of this file that
 * were not modified remain subject to the original copyright notice and license, and the original LICENSE is kept in
 * LICENSE_UPSTREAM located alongside LICENSE.
 *
 * The complete record of changes to this file is publicly available in the project's Git history, which anyone may view
 * at any time for any reason.
 *
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// spotless:on

package dev.aoqia.leaf.loom.configuration.providers.zomboid;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public abstract sealed class ZomboidJar permits ZomboidJar.Client, ZomboidJar.ClientOnly, ZomboidJar.Common,
    ZomboidJar.Merged, ZomboidJar.Server, ZomboidJar.Complete
{
    private final boolean merged, client, server;

    private final Path path;

    private final Type type;

    protected ZomboidJar(Path path, boolean merged, boolean client, boolean server, Type type) {
        this.path = Objects.requireNonNull(path);
        this.merged = merged;
        this.client = client;
        this.server = server;
        this.type = type;
    }

    public abstract ZomboidJar forPath(Path path);

    public String getName() {
        return type.toString();
    }

    public Path getPath() {
        return path;
    }

    public Type getType() {
        return type;
    }

    public boolean includesClient() {
        return client;
    }

    public boolean includesServer() {
        return server;
    }

    public boolean isMerged() {
        return merged;
    }

    public File toFile() {
        return getPath().toFile();
    }

    // Un-split client jar
    public static final class Client extends ZomboidJar {
        public Client(Path path) {
            super(path, false, true, false, Type.CLIENT);
        }

        @Override
        public ZomboidJar forPath(Path path) {
            return new Client(path);
        }
    }

    // Split client jar
    public static final class ClientOnly extends ZomboidJar {
        public ClientOnly(Path path) {
            super(path, false, true, false, Type.CLIENT_ONLY);
        }

        @Override
        public ZomboidJar forPath(Path path) {
            return new ClientOnly(path);
        }
    }

    public static final class Common extends ZomboidJar {
        public Common(Path path) {
            super(path, false, false, true, Type.COMMON);
        }

        @Override
        public ZomboidJar forPath(Path path) {
            return new Common(path);
        }
    }

    public static final class Complete extends ZomboidJar {
        public Complete(Path path) {
            super(path, false, true, true, Type.COMPLETE);
        }

        @Override
        public ZomboidJar forPath(Path path) {
            return new Complete(path);
        }
    }

    public static final class Merged extends ZomboidJar {
        public Merged(Path path) {
            super(path, true, true, true, Type.MERGED);
        }

        @Override
        public ZomboidJar forPath(Path path) {
            return new Merged(path);
        }
    }

    public static final class Server extends ZomboidJar {
        public Server(Path path) {
            super(path, false, false, true, Type.SERVER);
        }

        @Override
        public ZomboidJar forPath(Path path) {
            return new Server(path);
        }
    }

    public enum Type {
        CLIENT("client"),

        CLIENT_ONLY("clientOnly"),
        // Split jars
        COMMON("common"),

        // Regular jar which contains client and server for both client and server installs
        // not merged or split
        COMPLETE("complete"),

        // Merged jar
        MERGED("merged"),
        // Regular jars, not merged or split
        SERVER("server");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
