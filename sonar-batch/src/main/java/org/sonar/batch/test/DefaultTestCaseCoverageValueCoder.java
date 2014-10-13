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
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseCoverage;
import org.sonar.batch.scan.filesystem.InputPathCache;

import java.util.ArrayList;
import java.util.List;

class DefaultTestCaseCoverageValueCoder implements ValueCoder {

  private InputPathCache inputPathCache;

  public DefaultTestCaseCoverageValueCoder(InputPathCache inputPathCache) {
    this.inputPathCache = inputPathCache;
  }

  @Override
  public void put(Value value, Object object, CoderContext context) {
    DefaultTestCaseCoverage t = (DefaultTestCaseCoverage) object;
    value.putUTF(((DefaultInputFile) t.testFile()).moduleKey());
    value.putUTF(((DefaultInputFile) t.testFile()).relativePath());
    value.putUTF(t.testName());
    value.putUTF(((DefaultInputFile) t.coveredFile()).moduleKey());
    value.putUTF(((DefaultInputFile) t.coveredFile()).relativePath());
    value.put(t.coveredLines().size());
    for (Integer line : t.coveredLines()) {
      value.put(line.intValue());
    }
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    String testModuleKey = value.getString();
    String testRelativePath = value.getString();
    InputFile testFile = inputPathCache.getFile(testModuleKey, testRelativePath);
    if (testFile == null) {
      throw new IllegalStateException("Unable to load InputFile " + testModuleKey + ":" + testRelativePath);
    }
    String name = value.getString();
    String mainModuleKey = value.getString();
    String mainRelativePath = value.getString();
    InputFile mainFile = inputPathCache.getFile(mainModuleKey, mainRelativePath);
    if (mainFile == null) {
      throw new IllegalStateException("Unable to load InputFile " + mainModuleKey + ":" + mainRelativePath);
    }
    int size = value.getInt();
    List<Integer> lines = new ArrayList<Integer>(size);
    for (int i = 0; i < size; i++) {
      lines.add(value.getInt());
    }
    return new DefaultTestCaseCoverage()
      .testFile(testFile)
      .testName(name)
      .cover(mainFile)
      .onLines(lines);
  }
}
