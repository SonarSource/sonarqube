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

import org.sonar.api.batch.sensor.internal.SensorStorage;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.dependency.Dependency;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DependencySensorTest {

  private DependencySensor sensor;
  private SensorContext context = mock(SensorContext.class);
  private DefaultFileSystem fileSystem;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new DependencySensor();
    fileSystem = new DefaultFileSystem(baseDir.toPath());
    when(context.fileSystem()).thenReturn(fileSystem);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoDepsFile() {
    DefaultInputFile file = new DefaultInputFile("foo", "src/foo.xoo").setLanguage("xoo")
      .setType(Type.MAIN);
    fileSystem.add(file);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File deps = new File(baseDir, "src/foo.xoo.deps");
    FileUtils.write(deps, "src/foo2.xoo:2\nsrc2/foo3.xoo:6\n\n#comment");
    DefaultInputFile inputFile1 = new DefaultInputFile("foo", "src/foo.xoo").setLanguage("xoo");
    DefaultInputFile inputFile2 = new DefaultInputFile("foo", "src/foo2.xoo").setLanguage("xoo");
    DefaultInputFile inputFile3 = new DefaultInputFile("foo", "src2/foo3.xoo").setLanguage("xoo");
    fileSystem.add(inputFile1);
    fileSystem.add(inputFile2);
    fileSystem.add(inputFile3);

    final SensorStorage sensorStorage = mock(SensorStorage.class);

    when(context.newDependency()).thenAnswer(new Answer<Dependency>() {
      @Override
      public Dependency answer(InvocationOnMock invocation) throws Throwable {
        return new DefaultDependency(sensorStorage);
      }
    });

    sensor.execute(context);

    verify(sensorStorage).store(new DefaultDependency()
      .from(inputFile1)
      .to(inputFile2)
      .weight(2));
    verify(sensorStorage).store(new DefaultDependency()
      .from(inputFile1)
      .to(inputFile3)
      .weight(6));
  }
}
