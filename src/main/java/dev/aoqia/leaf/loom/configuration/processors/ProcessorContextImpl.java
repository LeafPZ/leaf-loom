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
 * Copyright (c) 2022-2023 FabricMC
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

package dev.aoqia.leaf.loom.configuration.processors;

import java.util.Objects;

import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.TinyRemapper;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.api.processor.ProcessorContext;
import dev.aoqia.leaf.loom.configuration.ConfigContext;
import dev.aoqia.leaf.loom.configuration.providers.mappings.MappingConfiguration;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidJar;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidJarConfiguration;
import dev.aoqia.leaf.loom.util.LazyCloseable;

public record ProcessorContextImpl(ConfigContext configContext, ZomboidJar minecraftJar) implements ProcessorContext {
    @Override
    public ZomboidJarConfiguration getJarConfiguration() {
        return configContext.extension().getZomboidJarConfiguration().get();
    }

    @Override
    public boolean isMerged() {
        return minecraftJar.isMerged();
    }

    @Override
    public boolean includesClient() {
        return minecraftJar.includesClient();
    }

    @Override
    public boolean includesServer() {
        return minecraftJar.includesServer();
    }

    @Override
    public LazyCloseable<TinyRemapper> createRemapper(MappingsNamespace from, MappingsNamespace to) {
        return ContextImplHelper.createRemapper(configContext, from, to);
    }

    @Override
    public MemoryMappingTree getMappings() {
        LoomGradleExtension extension = LoomGradleExtension.get(configContext().project());
        MappingConfiguration mappingConfiguration = Objects.requireNonNull(extension.getMappingConfigurationOrNull(),
            "Mappings have not been configured");
        return mappingConfiguration.getMappingsService(configContext().project(), configContext().serviceFactory())
            .getMappingTree();
    }

    @Override
    public boolean disableObfuscation() {
        return configContext().extension().disableObfuscation();
    }

    @Override
    public MappingsNamespace getProductionNamespace() {
        return configContext().extension().getProductionNamespaceEnum().get();
    }
}
