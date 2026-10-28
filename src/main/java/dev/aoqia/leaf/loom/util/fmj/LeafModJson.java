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

package dev.aoqia.leaf.loom.util.fmj;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import static dev.aoqia.leaf.loom.util.fmj.LeafModJsonUtils.readString;

public abstract sealed class LeafModJson permits LeafModJsonV1, LeafModJsonV2, LeafModJson.Mockable {
    protected final JsonObject jsonObject;

    private final LeafModJsonSource source;

    protected LeafModJson(JsonObject jsonObject, LeafModJsonSource source) {
        this.jsonObject = Objects.requireNonNull(jsonObject);
        this.source = Objects.requireNonNull(source);
    }

    public abstract Map<String, ModEnvironment> getClassTweakers();

    @Nullable
    public abstract JsonElement getCustom(String key);

    public String getId() {
        return readString(jsonObject, "id");
    }

    public abstract List<String> getMixinConfigurations();

    public String getModVersion() {
        return readString(jsonObject, "version");
    }

    public LeafModJsonSource getSource() {
        return source;
    }

    public abstract int getVersion();

    @Override
    public final int hashCode() {
        return Objects.hash(getId(), getVersion());
    }

    @Override
    public final String toString() {
        return getClass().getName()
            + "[id=%s, version=%s, classTweakers=%s]".formatted(getId(), getVersion(), getClassTweakers());
    }

    @VisibleForTesting
    public abstract non-sealed class Mockable extends LeafModJson {
        private Mockable() {
            super(null, null);
            throw new AssertionError();
        }
    }
}
