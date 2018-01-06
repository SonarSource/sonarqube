/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.xoo.coverage;

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

public class OverallCoverageSensorTest {

  private OverallCoverageSensor sensor;
  private SensorContextTester context;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new OverallCoverageSensor();
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoCoverageFile() {
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testLineHitNoConditions() throws IOException {
    File coverage = new File(baseDir, "src/foo.xoo.overallcoverage");
    FileUtils.write(coverage, "1:3\n\n#comment");
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).setLines(10).build();
    context.fileSystem().add(inputFile);

    sensor.execute(context);

    assertThat(context.lineHits("foo:src/foo.xoo", 1)).isEqualTo(3);
  }

  @Test
  public void testLineHitAndConditions() throws IOException {
    File coverage = new File(baseDir, "src/foo.xoo.overallcoverage");
    FileUtils.write(coverage, "1:3:4:2");
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).setLines(10).build();
    context.fileSystem().add(inputFile);

    sensor.execute(context);

    assertThat(context.lineHits("foo:src/foo.xoo", 1)).isEqualTo(3);
    assertThat(context.conditions("foo:src/foo.xoo", 1)).isEqualTo(4);
    assertThat(context.coveredConditions("foo:src/foo.xoo", 1)).isEqualTo(2);
  }
}
