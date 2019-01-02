/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class CpdTokenizerSensorTest {

  private CpdTokenizerSensor sensor;
  private SensorContextTester context;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new CpdTokenizerSensor();
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testExecution() throws IOException {
    String content = "public class Foo {\n\n}";

    createSourceFile(content);

    sensor.execute(context);

    assertThat(context.cpdTokens("foo:src/foo.xoo")).extracting("value", "startLine", "startUnit", "endUnit")
      .containsExactly(
        tuple("publicclassFoo{", 1, 1, 4),
        tuple("}", 3, 5, 5));
  }

  private void createSourceFile(String content) throws IOException {
    File sourceFile = new File(baseDir, "src/foo.xoo");
    FileUtils.write(sourceFile, content);
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage("xoo")
      .initMetadata(content)
      .setModuleBaseDir(baseDir.toPath())
      .build();
    context.fileSystem().add(inputFile);
  }

}
