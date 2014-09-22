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
package org.sonar.xoo.lang;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.test.TestCase;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CoveragePerTestSensorTest {

  private CoveragePerTestSensor sensor;
  private SensorContext context = mock(SensorContext.class);
  private DefaultFileSystem fileSystem;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new CoveragePerTestSensor();
    fileSystem = new DefaultFileSystem();
    when(context.fileSystem()).thenReturn(fileSystem);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfCoveragePerTestFile() {
    DefaultInputFile testFile = new DefaultInputFile("foo", "test/fooTest.xoo").setAbsolutePath(new File(baseDir, "test/fooTest.xoo").getAbsolutePath()).setLanguage("xoo")
      .setType(Type.TEST);
    fileSystem.add(testFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File coverPerTest = new File(baseDir, "test/fooTest.xoo.coveragePerTest");
    FileUtils.write(coverPerTest, "test1:src/foo.xoo:1,2,3,4\ntest2:src/foo.xoo:5,6,7\n\n#comment");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    DefaultInputFile testFile = new DefaultInputFile("foo", "test/fooTest.xoo").setAbsolutePath(new File(baseDir, "test/fooTest.xoo").getAbsolutePath()).setLanguage("xoo")
      .setType(Type.TEST);
    fileSystem.add(inputFile);
    fileSystem.add(testFile);

    TestCase test1 = new DefaultTestCaseBuilder(testFile, "test1").durationInMs(10).build();
    TestCase test2 = new DefaultTestCaseBuilder(testFile, "test2").durationInMs(10).build();
    when(context.getTestCase(testFile, "test1")).thenReturn(test1);
    when(context.getTestCase(testFile, "test2")).thenReturn(test2);

    sensor.execute(context);

    verify(context).saveCoveragePerTest(test1, inputFile, Arrays.asList(1, 2, 3, 4));
    verify(context).saveCoveragePerTest(test2, inputFile, Arrays.asList(5, 6, 7));
  }
}
