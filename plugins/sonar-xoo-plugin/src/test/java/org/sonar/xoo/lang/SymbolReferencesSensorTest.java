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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SymbolReferencesSensorTest {

  private SymbolReferencesSensor sensor;
  private SensorContextTester context;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File baseDir;
  private ResourcePerspectives perspectives;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder();
    perspectives = mock(ResourcePerspectives.class);
    sensor = new SymbolReferencesSensor(perspectives);
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
    FileUtils.write(symbol, "1:4,7\n12:15,23:33\n\n#comment");
    InputFile inputFile = new TestInputFileBuilder("foo", "src/foo.xoo").setLanguage("xoo").setModuleBaseDir(baseDir.toPath()).build();
    context.fileSystem().add(inputFile);
    Symbolizable symbolizable = mock(Symbolizable.class);
    when(perspectives.as(Symbolizable.class, inputFile)).thenReturn(symbolizable);
    Symbolizable.SymbolTableBuilder symbolTableBuilder = mock(Symbolizable.SymbolTableBuilder.class);
    when(symbolizable.newSymbolTableBuilder()).thenReturn(symbolTableBuilder);

    Symbol symbol1 = mock(Symbol.class);
    when(symbolTableBuilder.newSymbol(1, 4)).thenReturn(symbol1);
    Symbol symbol2 = mock(Symbol.class);
    when(symbolTableBuilder.newSymbol(12, 15)).thenReturn(symbol2);

    sensor.execute(context);

    verify(symbolTableBuilder).newSymbol(1, 4);
    verify(symbolTableBuilder).newReference(symbol1, 7);
    verify(symbolTableBuilder).newSymbol(12, 15);
    verify(symbolTableBuilder).newReference(symbol2, 23, 33);
  }

}
