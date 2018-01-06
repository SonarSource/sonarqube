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
package org.sonar.xoo.lang;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.source.Highlightable;
import org.sonar.api.source.Highlightable.HighlightingBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SyntaxHighlightingSensorTest {

  private SyntaxHighlightingSensor sensor;
  private SensorContextTester context;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;
  private ResourcePerspectives perspectives;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    perspectives = mock(ResourcePerspectives.class);
    sensor = new SyntaxHighlightingSensor(perspectives);
    context = SensorContextTester.create(baseDir);
  }

  @Test
  public void testDescriptor() {
    sensor.describe(new DefaultSensorDescriptor());
  }

  @Test
  public void testNoExecutionIfNoSyntaxFile() {
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage("xoo")
      .setModuleBaseDir(baseDir.toPath())
      .build();
    context.fileSystem().add(inputFile);
    sensor.execute(context);
  }

  @Test
  public void testExecution() throws IOException {
    File symbol = new File(baseDir, "src/foo.xoo.highlighting");
    FileUtils.write(symbol, "1:4:k\n12:15:cppd\n\n#comment");
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo")
      .setLanguage("xoo")
      .setModuleBaseDir(baseDir.toPath())
      .initMetadata(" xoo\nazertyazer\nfoo")
      .build();
    context.fileSystem().add(inputFile);

    Highlightable highlightable = mock(Highlightable.class);
    when(perspectives.as(Highlightable.class, inputFile)).thenReturn(highlightable);
    HighlightingBuilder builder = mock(Highlightable.HighlightingBuilder.class);
    when(highlightable.newHighlighting()).thenReturn(builder);

    sensor.execute(context);

    verify(builder).highlight(1, 4, "k");
    verify(builder).highlight(12, 15, "cppd");
    verify(builder).done();
  }
}
