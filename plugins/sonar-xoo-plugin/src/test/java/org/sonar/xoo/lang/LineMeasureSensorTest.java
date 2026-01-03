/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LineMeasureSensorTest {

  private LineMeasureSensor sensor;
  private SensorContextTester context;
  private FileLinesContextFactory contextFactory;
  private FileLinesContext fileLinesContext;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    contextFactory = mock(FileLinesContextFactory.class);
    fileLinesContext = mock(FileLinesContext.class);
    when(contextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    sensor = new LineMeasureSensor(contextFactory);
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void testDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    assertThat(descriptor.name()).isEqualTo("Xoo Line Measure Sensor");
    assertThat(descriptor.languages()).containsOnly("xoo");
  }

  @Test
  public void testNoExecutionIfNoMeasureFile() {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").build();
    context.fileSystem().add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File measures = new File(baseDir, "src/foo.xoo.linemeasures");
    FileUtils.write(measures, "ncloc_data:1=1;2=1;3=0\n\n#comment", StandardCharsets.UTF_8);
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);

    sensor.execute(context);

    verify(fileLinesContext).setIntValue("ncloc_data", 1, 1);
    verify(fileLinesContext).setIntValue("ncloc_data", 2, 1);
    verify(fileLinesContext).setIntValue("ncloc_data", 3, 0);
    verify(fileLinesContext).save();
  }

  @Test
  public void testExecutionWithBlankLines() throws IOException {
    File measures = new File(baseDir, "src/foo.xoo.linemeasures");
    FileUtils.write(measures, "\n\nncloc_data:1=1\n\n", StandardCharsets.UTF_8);
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);

    sensor.execute(context);

    verify(fileLinesContext).setIntValue("ncloc_data", 1, 1);
    verify(fileLinesContext).save();
  }

  @Test
  public void failIfInvalidFormat() throws IOException {
    File measures = new File(baseDir, "src/foo.xoo.linemeasures");
    FileUtils.write(measures, "invalid_format", StandardCharsets.UTF_8);
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);

    assertThatThrownBy(() -> sensor.execute(context))
      .isInstanceOf(IllegalStateException.class);
  }
}
