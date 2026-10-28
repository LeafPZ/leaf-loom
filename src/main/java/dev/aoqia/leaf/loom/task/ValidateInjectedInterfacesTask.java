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
 * Copyright (c) 2026 FabricMC
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

package dev.aoqia.leaf.loom.task;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.problems.ProblemId;
import org.gradle.api.problems.Problems;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.classtweaker.api.ClassTweakerReader;
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.configuration.ifaceinject.InterfaceInjectionProcessor;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidSourceSets;
import dev.aoqia.leaf.loom.util.Constants;
import dev.aoqia.leaf.loom.util.FileSystemUtil;
import dev.aoqia.leaf.loom.util.fmj.LeafModJson;
import dev.aoqia.leaf.loom.util.fmj.LeafModJsonFactory;
import dev.aoqia.leaf.loom.util.gradle.SourceSetHelper;
import dev.aoqia.leaf.loom.util.problem.LoomProblemReporter;
import dev.aoqia.leaf.loom.util.problem.LoomProblems;
import dev.aoqia.leaf.loom.util.problem.ProblemReportingOptions;

/**
 * Checks that injected interfaces are valid, i.e. that all their instance methods have a default implementation.
 *
 * <p>
 * {@snippet lang = groovy:
 * tasks.register('validateInjectedInterfaces', dev.aoqia.leaf.loom.task.ValidateInjectedInterfacesTask) {
 * 	// By default, this task is set up for the default mod jar - the "jar" or "remapJar" task depending
 * 	// on the project configuration - and the main source set. To modify the defaults:
 * 	modJar = tasks.named('otherJar').flatMap { it.archiveFile }
 * 	source(sourceSets.other) // Add a new source set.
 * }
 * }
 */
@DisableCachingByDefault
public abstract class ValidateInjectedInterfacesTask extends DefaultTask {
    private static final ProblemId ABSTRACT_METHOD_IN_INJECTED_INTERFACE = LoomProblems
        .problemId("abstract-method-in-injected-interface", "Abstract method in injected interface");

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateInjectedInterfacesTask.class);

    private final ProblemReportingOptions problemReportingOptions;

    public ValidateInjectedInterfacesTask() {
        setGroup(JavaBasePlugin.VERIFICATION_GROUP);
        problemReportingOptions = getProject().getObjects().newInstance(ProblemReportingOptions.class);
        configureForDefaultSetup();

        // Ignore outputs for up-to-date checks as there aren't any (so only inputs are checked)
        getOutputs().upToDateWhen(task -> true);
    }

    private static void checkInjectedInterface(byte[] classBytes, FileCollection sourceRoots,
        Consumer<Violation> violationConsumer)
    {
        ClassVisitor visitor = new ClassVisitor(Constants.ASM_VERSION) {
            private @Nullable String packageName;
            private @Nullable String simpleClassName;
            private @Nullable String sourceFileName;

            @Override
            public void visit(int version, int access, String name, @Nullable String signature,
                @Nullable String superName, String @Nullable [] interfaces)
            {
                // Only the simple name is used in the error messages to make them more concise.
                // The package is needed for resolving the source code file based on package + file name.
                int slashIndex = name.lastIndexOf('/');

                if (slashIndex >= 0) {
                    simpleClassName = name.substring(slashIndex + 1);
                    packageName = name.substring(0, slashIndex);
                } else {
                    simpleClassName = name;
                    packageName = null;
                }
            }

            @Override
            public @Nullable MethodVisitor visitMethod(int access, String name, String descriptor,
                @Nullable String signature, String @Nullable [] exceptions)
            {
                if (Modifier.isAbstract(access)) {
                    final File sourceFile = resolveSourceFile(simpleClassName, packageName, sourceFileName,
                        sourceRoots);
                    violationConsumer.accept(new Violation(simpleClassName, name, descriptor, sourceFile));
                }

                return null;
            }

            @Override
            public void visitSource(@Nullable String source, @Nullable String debug) {
                sourceFileName = source;
            }
        };
        new ClassReader(classBytes).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
    }

    private static void findInjectedInterfacesFromClassTweaker(byte[] ctBytes, Consumer<String> consumer) {
        ClassTweakerVisitor visitor = new ClassTweakerVisitor() {
            @Override
            public void visitInjectedInterface(String owner, String iface, boolean transitive) {
                // Strip generics in case we have a signature instead of a class name
                int genericsIndex = iface.indexOf('<');

                if (genericsIndex >= 0) {
                    iface = iface.substring(0, genericsIndex);
                }

                consumer.accept(iface);
            }
        };

        ClassTweakerReader.create(visitor).read(ctBytes);
    }

    private static @Nullable File resolveSourceFile(String className, @Nullable String packageName,
        @Nullable String sourceFileName, FileCollection sourceRoots)
    {
        if (sourceFileName == null) {
            LOGGER.warn("No source file name present for injected interface {} (package {})", className, packageName);
            return null;
        }

        String relativeSourcePath = packageName != null ? packageName + File.separator + sourceFileName
            : sourceFileName;

        for (File sourceRoot : sourceRoots) {
            File sourceFile = new File(sourceRoot, relativeSourcePath);

            if (sourceFile.exists()) {
                return sourceFile;
            }
        }

        return null;
    }

    /**
     * The mod jar to check.
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getModJar();

    @Nested
    public ProblemReportingOptions getProblemReportingOptions() {
        return problemReportingOptions;
    }

    /**
     * A collection of source code roots where the mod jar was built from.
     * This is used for resolving the corresponding source code files where report details are attached.
     *
     * <p>
     * Adding source roots is optional. If not added, the file paths simply won't show up in error reports.
     *
     * <p>
     * Sources from source sets can be added with {@link #source}. By default, the {@code main}
     * and {@code client} (if using split source sets) will be present in this collection.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract ConfigurableFileCollection getSourceRoots();

    public void problemReportingOptions(Action<? super ProblemReportingOptions> action) {
        action.execute(getProblemReportingOptions());
    }

    /**
     * Adds all sources from a {@link SourceSet} to the {@linkplain #getSourceRoots() source roots}
     * for error messages.
     *
     * @param sourceSet the source set to add
     */
    public void source(SourceSet sourceSet) {
        getSourceRoots().from(sourceSet.getAllSource().getSrcDirs());
    }

    @TaskAction
    protected void check() throws IOException {
        final WorkQueue workQueue = getWorkerExecutor().noIsolation();

        workQueue.submit(ValidateInjectedInterfacesAction.class, params -> {
            params.getModJar().set(getModJar());
            params.getSourceRoots().from(getSourceRoots());
            params.getProblemReportingOptions().set(getProblemReportingOptions());
        });
    }

    @ApiStatus.Internal
    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    private void configureForDefaultSetup() {
        final LoomGradleExtension extension = LoomGradleExtension.get(getProject());

        if (extension.dontRemapOutputs()) {
            getModJar().convention(
                getProject().getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class).flatMap(Jar::getArchiveFile));
        } else {
            getModJar().convention(getProject().getTasks()
                .named(RemapTaskConfiguration.REMAP_JAR_TASK_NAME, Jar.class)
                .flatMap(Jar::getArchiveFile));
        }

        source(SourceSetHelper.getMainSourceSet(getProject()));

        if (extension.areEnvironmentSourceSetsSplit()) {
            source(
                SourceSetHelper.getSourceSetByName(ZomboidSourceSets.Split.CLIENT_ONLY_SOURCE_SET_NAME, getProject()));
        }
    }

    @ApiStatus.Internal
    public abstract static class ValidateInjectedInterfacesAction
        implements WorkAction<ValidateInjectedInterfacesParams>
    {
        @Override
        public void execute() {
            try {
                check();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Inject
        protected abstract Problems getProblems();

        private void check() throws IOException {
            List<Violation> violations = new ArrayList<>();
            Path modJar = getParameters().getModJar().get().getAsFile().toPath();
            LeafModJson modJson = LeafModJsonFactory.createFromZip(modJar);
            Set<String> injectedInterfaces = new HashSet<>();

            // Look for injected interfaces in fabric.mod.json "custom" section
            for (var injectedInterface : InterfaceInjectionProcessor.InjectedInterface.fromMod(modJson)) {
                injectedInterfaces.add(injectedInterface.ifaceName());
            }

            try (FileSystemUtil.Delegate fs = FileSystemUtil.getJarFileSystem(modJar)) {
                // Look for injected interfaces in class tweakers
                for (String classTweaker : modJson.getClassTweakers().keySet()) {
                    byte[] ctBytes = Files.readAllBytes(fs.getPath(classTweaker));
                    findInjectedInterfacesFromClassTweaker(ctBytes, injectedInterfaces::add);
                }

                // Check injected interfaces
                for (String itf : injectedInterfaces) {
                    Path interfacePath = fs.getPath(itf + ".class");

                    if (!Files.exists(interfacePath)) {
                        LOGGER.info("Injected interface {} not found in mod jar {}, skipping validation", itf, modJar);
                        continue;
                    }

                    byte[] classBytes = Files.readAllBytes(interfacePath);
                    checkInjectedInterface(classBytes, getParameters().getSourceRoots(), violations::add);
                }
            }

            var reporter = new LoomProblemReporter(getProblems().getReporter(),
                getParameters().getProblemReportingOptions().get());

            for (Violation violation : violations) {
                reporter.problem(ABSTRACT_METHOD_IN_INJECTED_INTERFACE, builder -> {
                    builder.contextualLabel(
                        "%s.%s%s".formatted(violation.itf, violation.methodName, violation.methodDesc));
                    builder.message("Injected interface %s has abstract method %s%s".formatted(violation.itf,
                        violation.methodName, violation.methodDesc));
                    builder.details(("Method %s.%s%s is abstract.\nAll injected interface methods must have a default "
                        + "implementation.").formatted(violation.itf, violation.methodName, violation.methodDesc));
                    builder.solution("Add a default implementation to the method.");

                    if (violation.sourceFile != null) {
                        builder.fileLocation(violation.sourceFile.toPath());
                    }
                });
            }

            reporter.reportAndThrow(ABSTRACT_METHOD_IN_INJECTED_INTERFACE);
        }
    }

    @ApiStatus.Internal
    public interface ValidateInjectedInterfacesParams extends WorkParameters {
        RegularFileProperty getModJar();

        Property<ProblemReportingOptions> getProblemReportingOptions();

        ConfigurableFileCollection getSourceRoots();
    }

    private record Violation(String itf, String methodName, String methodDesc, @Nullable File sourceFile) {}
}
