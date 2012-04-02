/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.squid;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.utils.SonarException;

public class JavaSourceImporterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private JavaSourceImporter importer;
  private SensorContext context;
  private InputFile inputFile;

  @Before
  public void init() {
    File fileToImport = new File("test-resources/rules/UndocumentedApi.java");
    inputFile = mock(InputFile.class);
    when(inputFile.getRelativePath()).thenReturn("UndocumentedApi.java");
    when(inputFile.getFile()).thenReturn(fileToImport);
    when(inputFile.getFileBaseDir()).thenReturn(fileToImport.getParentFile());
    importer = new JavaSourceImporter(true);
    context = mock(SensorContext.class);
  }

  @Test
  public void shouldSetSource() throws IOException {
    JavaFile javaFile = JavaFile.fromRelativePath("UndocumentedApi.java", true);
    when(context.isIndexed(javaFile, true)).thenReturn(true);
    importer.importSource(context, javaFile, inputFile, Charset.defaultCharset());

    verify(context).saveSource(eq(javaFile), anyString());
  }

  @Test
  public void testImportUnexistingFile() {
    File fileToImport = new File("unexisting-file.java");
    inputFile = mock(InputFile.class);
    when(inputFile.getRelativePath()).thenReturn("UndocumentedApi.java");
    when(inputFile.getFile()).thenReturn(fileToImport);
    when(inputFile.getFileBaseDir()).thenReturn(fileToImport.getParentFile());

    JavaFile javaFile = JavaFile.fromRelativePath("UndocumentedApi.java", true);
    when(context.isIndexed(javaFile, true)).thenReturn(true);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Unable to read and import the source file");
    thrown.expectMessage("unexisting-file.java");
    importer.importSource(context, javaFile, inputFile, Charset.defaultCharset());
  }

  /**
   * SONAR-3315
   */
  @Test
  public void testDuplicateSource() {
    JavaFile javaFile = JavaFile.fromRelativePath("UndocumentedApi.java", true);
    when(context.isIndexed(javaFile, true)).thenReturn(true);
    Mockito.doThrow(new SonarException("Duplicate source for resource")).when(context).saveSource(any(JavaFile.class), anyString());

    thrown.expect(SonarException.class);
    thrown.expectMessage("Duplicate source for resource");
    thrown.expectMessage(", on file:");
    thrown.expectMessage("UndocumentedApi.java");
    importer.importSource(context, javaFile, inputFile, Charset.defaultCharset());
  }

}
