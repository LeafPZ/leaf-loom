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
 * Copyright (c) 2021 FabricMC
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

package dev.aoqia.leaf.loom.configuration.accesswidener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dev.aoqia.leaf.loom.util.ZipUtils;

public record AccessWidenerFile(String path, String modId, byte[] content) {
    /**
     * Reads the access-widener contained in a mod jar, or returns null if there is none.
     */
    public static AccessWidenerFile fromModJar(Path modJarPath) {
        byte[] modJsonBytes;

        try {
            // TODO(leaf): Make compatible with leaf.mod.json
            modJsonBytes = ZipUtils.unpackNullable(modJarPath, "leaf.mod.json");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read access-widener file from: " + modJarPath.toAbsolutePath(),
                e);
        }

        if (modJsonBytes == null) {
            return null;
        }

        JsonObject jsonObject = new Gson().fromJson(new String(modJsonBytes, StandardCharsets.UTF_8), JsonObject.class);

        if (!jsonObject.has("accessWidener")) {
            return null;
        }

        String awPath = jsonObject.get("accessWidener").getAsString();
        String modId = jsonObject.get("id").getAsString();

        byte[] content;

        try {
            content = ZipUtils.unpack(modJarPath, awPath);
        } catch (IOException e) {
            throw new UncheckedIOException(
                "Could not find access widener file (%s) defined in the leaf.mod.json file of %s".formatted(awPath,
                    modJarPath.toAbsolutePath()),
                e);
        }

        return new AccessWidenerFile(awPath, modId, content);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(path, modId);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }
}
