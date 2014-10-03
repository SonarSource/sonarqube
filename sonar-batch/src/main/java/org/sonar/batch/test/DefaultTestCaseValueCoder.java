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
package org.sonar.batch.test;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.test.TestCase;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCase;
import org.sonar.batch.scan.filesystem.InputPathCache;

import javax.annotation.Nullable;

class DefaultTestCaseValueCoder implements ValueCoder {

  private InputPathCache inputPathCache;

  public DefaultTestCaseValueCoder(InputPathCache inputPathCache) {
    this.inputPathCache = inputPathCache;
  }

  public void put(Value value, Object object, CoderContext context) {
    DefaultTestCase t = (DefaultTestCase) object;
    value.putUTF(((DefaultInputFile) t.testFile()).moduleKey());
    value.putUTF(((DefaultInputFile) t.testFile()).relativePath());
    value.putUTF(t.name());
    putUTFOrNull(value, t.message());
    putUTFOrNull(value, t.stackTrace());
    Long durationInMs = t.durationInMs();
    value.put(durationInMs != null ? durationInMs.longValue() : -1);
    value.put(t.type().ordinal());
    value.put(t.status().ordinal());
  }

  private void putUTFOrNull(Value value, @Nullable String utfOrNull) {
    if (utfOrNull != null) {
      value.putUTF(utfOrNull);
    } else {
      value.putNull();
    }
  }

  public Object get(Value value, Class clazz, CoderContext context) {
    String moduleKey = value.getString();
    String relativePath = value.getString();
    InputFile testFile = inputPathCache.getFile(moduleKey, relativePath);
    if (testFile == null) {
      throw new IllegalStateException("Unable to load InputFile " + moduleKey + ":" + relativePath);
    }
    String name = value.getString();
    String message = value.getString();
    String stack = value.getString();
    long duration = value.getLong();
    TestCase.Type type = TestCase.Type.values()[value.getInt()];
    TestCase.Status status = TestCase.Status.values()[value.getInt()];
    return new DefaultTestCase()
      .inTestFile(testFile)
      .ofType(type)
      .name(name)
      .durationInMs(duration != -1 ? duration : null)
      .status(status)
      .message(message)
      .stackTrace(stack);
  }
}
