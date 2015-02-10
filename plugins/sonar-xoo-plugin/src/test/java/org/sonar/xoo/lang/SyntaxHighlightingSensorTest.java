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
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.HighlightingBuilder;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyntaxHighlightingSensorTest {

  private SyntaxHighlightingSensor sensor;
  private SensorContext context = mock(SensorContext.class);
  private DefaultFileSystem fileSystem;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    sensor = new SyntaxHighlightingSensor();
    fileSystem = new DefaultFileSystem(baseDir);
    when(context.fileSystem()).thenReturn(fileSystem);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoSyntaxFile() {
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    fileSystem.add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File symbol = new File(baseDir, "src/foo.xoo.highlighting");
    FileUtils.write(symbol, "1:4:k\n12:15:cppd\n\n#comment");
    DefaultInputFile inputFile = new DefaultInputFile("foo", "src/foo.xoo").setAbsolutePath(new File(baseDir, "src/foo.xoo").getAbsolutePath()).setLanguage("xoo");
    fileSystem.add(inputFile);
    HighlightingBuilder builder = mock(HighlightingBuilder.class);
    when(context.highlightingBuilder(inputFile)).thenReturn(builder);

    sensor.execute(context);

    verify(builder).highlight(1, 4, TypeOfText.KEYWORD);
    verify(builder).highlight(12, 15, TypeOfText.CPP_DOC);
    verify(builder).done();
  }
}
