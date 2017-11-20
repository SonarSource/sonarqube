/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.xoo.lang;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SymbolReferencesSensorTest {

  private SymbolReferencesSensor sensor;
  private SensorContextTester context;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new SymbolReferencesSensor();
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoSymbolFile() {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File symbol = new File(baseDir, "src/foo.xoo.symbol");
    FileUtils.write(symbol, "1:0:1:3,2:2:2:5,3:0:3:3\n\n#comment");
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage("xoo")
      .setModuleBaseDir(baseDir.toPath())
      .initMetadata("xoo\n  xoo\nxoo")
      .build();
    context.fileSystem().add(inputFile);

    sensor.execute(context);

    assertThat(context.referencesForSymbolAt("foo:src/foo.xoo", 1, 2)).extracting("start.line", "start.lineOffset", "end.line", "end.lineOffset")
      .containsExactly(tuple(2, 2, 2, 5), tuple(3, 0, 3, 3));
  }

}
