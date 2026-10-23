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
 * Copyright (c) 2025 FabricMC
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

package dev.aoqia.leaf.loom.configuration.providers.mappings.extras.annotations;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

class TypeAnnotationNodeSerializer implements JsonSerializer<TypeAnnotationNode>, JsonDeserializer<TypeAnnotationNode> {
	@Override
	public TypeAnnotationNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		AnnotationNode annotation = context.deserialize(json, AnnotationNode.class);
		JsonObject jsonObject = json.getAsJsonObject();
		int typeRef = jsonObject.getAsJsonPrimitive("type_ref").getAsInt();
		JsonPrimitive typePathPrimitive = jsonObject.getAsJsonPrimitive("type_path");
		String typePath = typePathPrimitive == null ? "" : typePathPrimitive.getAsString();
		TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, TypePath.fromString(typePath), annotation.desc);
		annotation.accept(typeAnnotation);
		return typeAnnotation;
	}

	@Override
	public JsonElement serialize(TypeAnnotationNode src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject json = context.serialize(src, AnnotationNode.class).getAsJsonObject();
		json.addProperty("type_ref", src.typeRef);

		if (src.typePath != null) {
			json.addProperty("type_path", src.typePath.toString());
		}

		return json;
	}
}
