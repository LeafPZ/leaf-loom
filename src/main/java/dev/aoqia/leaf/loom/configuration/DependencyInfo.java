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
 * Copyright (c) 2016-2021 FabricMC
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

package dev.aoqia.leaf.loom.configuration;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;

public class DependencyInfo {
    final Dependency dependency;

    final Project project;

    final Configuration sourceConfiguration;

    private String resolvedVersion = null;

    DependencyInfo(Project project, Dependency dependency, Configuration sourceConfiguration) {
        this.project = project;
        this.dependency = dependency;
        this.sourceConfiguration = sourceConfiguration;
    }

    public static DependencyInfo create(Project project, Configuration configuration) {
        DependencySet dependencies = configuration.getDependencies();

        if (dependencies.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Configuration '%s' has no dependencies", configuration.getName()));
        }

        if (dependencies.size() != 1) {
            throw new IllegalArgumentException(
                String.format("Configuration '%s' must only have 1 dependency", configuration.getName()));
        }

        return create(project, dependencies.iterator().next(), configuration);
    }

    public static DependencyInfo create(Project project, Dependency dependency, Configuration sourceConfiguration) {
        if (dependency instanceof FileCollectionDependency fileCollectionDependency) {
            return new FileDependencyInfo(project, fileCollectionDependency, sourceConfiguration);
        } else {
            return new DependencyInfo(project, dependency, sourceConfiguration);
        }
    }

    public static DependencyInfo create(Project project, String configuration) {
        return create(project, project.getConfigurations().getByName(configuration));
    }

    public Dependency getDependency() {
        return dependency;
    }

    public String getDepString() {
        return dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion();
    }

    public String getResolvedDepString() {
        return dependency.getGroup() + ":" + dependency.getName() + ":" + getResolvedVersion();
    }

    public String getResolvedVersion() {
        if (resolvedVersion != null) {
            return resolvedVersion;
        }

        for (ResolvedDependency rd : sourceConfiguration.getResolvedConfiguration().getFirstLevelModuleDependencies()) {
            if (rd.getModuleGroup().equals(dependency.getGroup()) && rd.getModuleName().equals(dependency.getName())) {
                resolvedVersion = rd.getModuleVersion();
                return resolvedVersion;
            }
        }

        resolvedVersion = dependency.getVersion();
        return resolvedVersion;
    }

    public Configuration getSourceConfiguration() {
        return sourceConfiguration;
    }

    public Set<File> resolve() {
        return sourceConfiguration.getIncoming()
            .artifactView(view -> view.componentFilter(this::matches))
            .getFiles()
            .getFiles();
    }

    public Optional<File> resolveFile() {
        Set<File> files = resolve();

        if (files.isEmpty()) {
            return Optional.empty();
        } else if (files.size() > 1) {
            StringBuilder builder = new StringBuilder(this.toString());
            builder.append(" resolves to more than one file:");

            for (File f : files) {
                builder.append("\n\t-").append(f.getAbsolutePath());
            }

            throw new RuntimeException(builder.toString());
        } else {
            return files.stream().findFirst();
        }
    }

    @Override
    public String toString() {
        return getDepString();
    }

    private boolean matches(ComponentIdentifier identifier) {
        if (identifier instanceof ModuleComponentIdentifier moduleComponentIdentifier) {
            return moduleComponentIdentifier.getGroup().equals(dependency.getGroup())
                && moduleComponentIdentifier.getModule().equals(dependency.getName())
                && moduleComponentIdentifier.getVersion().equals(dependency.getVersion());
        }

        return false;
    }
}
