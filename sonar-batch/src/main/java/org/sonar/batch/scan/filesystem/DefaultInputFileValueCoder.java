/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.filesystem;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;

import javax.annotation.Nullable;

import java.io.File;

class DefaultInputFileValueCoder implements ValueCoder {

  @Override
  public void put(Value value, Object object, CoderContext context) {
    DeprecatedDefaultInputFile f = (DeprecatedDefaultInputFile) object;
    putUTFOrNull(value, f.moduleKey());
    putUTFOrNull(value, f.relativePath());
    value.putString(f.getFileBaseDir().toString());
    putUTFOrNull(value, f.deprecatedKey());
    value.putString(f.sourceDirAbsolutePath());
    putUTFOrNull(value, f.pathRelativeToSourceDir());
    putUTFOrNull(value, f.absolutePath());
    value.putString(f.language());
    value.putString(f.type().name());
    value.putString(f.status().name());
    putUTFOrNull(value, f.hash());
    value.put(f.lines());
    value.put(f.nonBlankLines());
    putUTFOrNull(value, f.encoding());
    value.put(f.isEmpty());
    value.putLongArray(f.originalLineOffsets());
    for (int i = 0; i < f.lines(); i++) {
      value.putByteArray(f.lineHashes()[i]);
    }
  }

  private void putUTFOrNull(Value value, @Nullable String utfOrNull) {
    if (utfOrNull != null) {
      value.putUTF(utfOrNull);
    } else {
      value.putNull();
    }
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    String moduleKey = value.getString();
    DeprecatedDefaultInputFile file = new DeprecatedDefaultInputFile(moduleKey, value.getString());
    file.setBasedir(new File(value.getString()));
    file.setDeprecatedKey(value.getString());
    file.setSourceDirAbsolutePath(value.getString());
    file.setPathRelativeToSourceDir(value.getString());
    file.setAbsolutePath(value.getString());
    file.setLanguage(value.getString());
    file.setType(InputFile.Type.valueOf(value.getString()));
    file.setStatus(InputFile.Status.valueOf(value.getString()));
    file.setHash(value.getString());
    file.setLines(value.getInt());
    file.setNonBlankLines(value.getInt());
    file.setEncoding(value.getString());
    file.setEmpty(value.getBoolean());
    file.setOriginalLineOffsets(value.getLongArray());
    byte[][] lineHashes = new byte[file.lines()][];
    for (int i = 0; i < file.lines(); i++) {
      lineHashes[i] = value.getByteArray();
    }
    file.setLineHashes(lineHashes);
    return file;
  }

}
