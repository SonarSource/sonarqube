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
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.duplication.DuplicationTokenBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.config.Settings;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class XooTokenizerSensorTest {

  private XooTokenizerSensor sensor;
  private SensorContext context = mock(SensorContext.class);
  private DefaultFileSystem fileSystem;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;
  private Settings settings;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new XooTokenizerSensor();
    fileSystem = new DefaultFileSystem(baseDir);
    when(context.fileSystem()).thenReturn(fileSystem);
    settings = new Settings();
    when(context.settings()).thenReturn(settings);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfExclusion() {
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    fileSystem.add(inputFile);
    settings.setProperty(CoreProperties.CPD_EXCLUSIONS, "**/foo.xoo");
    sensor.execute(context);
    verify(context, never()).duplicationTokenBuilder(any(InputFile.class));
  }

  @Test
  public void testExecution() throws IOException {
    File source = new File(baseDir, "src/foo.xoo");
    FileUtils.write(source, "token1 token2 token3\ntoken4");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    fileSystem.add(inputFile);
    DuplicationTokenBuilder builder = mock(DuplicationTokenBuilder.class);
    when(context.duplicationTokenBuilder(inputFile)).thenReturn(builder);

    sensor.execute(context);

    verify(builder).addToken(1, "token1");
    verify(builder).addToken(1, "token2");
    verify(builder).addToken(1, "token3");
    verify(builder).addToken(2, "token4");
    verify(builder).done();
  }
}
