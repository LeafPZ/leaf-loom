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
 * Copyright (c) 2024-2025 FabricMC
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

package dev.aoqia.leaf.loom.util.service;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * A record to hold the options and service class for a service.
 *
 * @param optionsClass The options class for the service.
 * @param serviceClass The service class.
 */
public record ServiceType<O extends Service.Options, S extends Service<O>>(
    Class<O> optionsClass,
    Class<S> serviceClass
)
{
    /**
     * Create an instance of the options class for the given service class.
     *
     * @param project The {@link Project} to create the options for.
     * @param action An action to configure the options.
     * @return The created options provider.
     */
    public Provider<O> create(Project project, Action<O> action) {
        return maybeCreate(project, o -> {
            action.execute(o);
            return true;
        });
    }

    /**
     * Maybe create an instance of the options class for the given service class.
     *
     * @param project The {@link Project} to create the options for.
     * @param function A function to configure the options, returning true if the options should be created.
     * @return The created options provider.
     */
    public Provider<O> maybeCreate(Project project, Function<O, Boolean> function) {
        return project.provider(() -> {
            O options = project.getObjects().newInstance(optionsClass);

            for (Method method : optionsClass.getDeclaredMethods()) {
                // Gradle property values are lazily initialized, ensure that all of the values are not null
                // Before we try to serialize the options as json
                method.invoke(options);
            }

            options.getServiceClass().set(serviceClass.getName());
            options.getServiceClass().finalizeValue();

            if (function.apply(options)) {
                return options;
            }

            return null;
        });
    }
}
