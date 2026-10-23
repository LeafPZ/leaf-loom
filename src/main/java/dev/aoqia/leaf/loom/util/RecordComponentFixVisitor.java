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

package dev.aoqia.leaf.loom.util;

import java.util.Objects;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.RecordComponentVisitor;

import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class RecordComponentFixVisitor extends ClassVisitor {
	private final MemoryMappingTree mappings;
	private final int officialNsId;
	private final int intermediaryNsId;

	private String owner;
	private boolean hasExistingComponents = false;

	public RecordComponentFixVisitor(ClassVisitor classVisitor, MemoryMappingTree mappings, int officialNsId, int intermediaryNsId) {
		super(Constants.ASM_VERSION, classVisitor);
		this.mappings = mappings;
		this.officialNsId = officialNsId;
		this.intermediaryNsId = intermediaryNsId;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.owner = name;

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		// Should never happen, but let's be safe
		hasExistingComponents = true;

		return super.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		String intermediaryName = Objects.requireNonNull(mappings.getField(owner, name, descriptor, officialNsId), "Could not get field for %s:%s%s".formatted(owner, name, descriptor)).getName(intermediaryNsId);

		if (!hasExistingComponents && intermediaryName != null && intermediaryName.startsWith("comp_")) {
			super.visitRecordComponent(name, descriptor, signature);
		}

		return super.visitField(access, name, descriptor, signature, value);
	}
}
