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
package org.sonar.java.ast.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.measures.Metric;

public class FileVisitorTest {

  private Squid squid;

  @Before
  public void setup() {
    squid = new Squid(new JavaSquidConfiguration());
  }

  @Test
  public void testExtractFileNameFromFilePath() {
    String filename = "/toto/tata/org/codehaus/sonar/MyClass.java";
    assertEquals("MyClass.java", FileVisitor.extractFileNameFromFilePath(filename));
  }

  @Test
  public void analyseTest003() {
    List<InputFile> files = Lists.newArrayList();
    files.add(SquidTestUtils.getInputFile("/metrics/loc/Test002.java"));
    files.add(SquidTestUtils.getInputFile("/metrics/classes/Test003.java"));
    squid.register(JavaAstScanner.class).scanFiles(files);
    SourceCode project = squid.aggregate();
    assertEquals(2, project.getInt(Metric.FILES));
    SourceCode defaultPackage = project.getFirstChild();
    SourceCode file = defaultPackage.getFirstChild();
    assertEquals("Test002.java", file.getKey());
    assertTrue(file.isType(SourceFile.class));
  }

  @Test
  public void createSourceFile() {
    SourceFile squidFile = FileVisitor.createSourceFile(new SourcePackage("org/sonar"), "Squid.java");
    assertEquals("org/sonar/Squid.java", squidFile.getKey());
  }

  @Test
  public void createSourceFileWithDefaultPackage() {
    SourceFile squidFile = FileVisitor.createSourceFile(new SourcePackage(""), "Squid.java");
    assertEquals("Squid.java", squidFile.getKey());
  }

  @Test
  public void createSourceFileWithNoPackage() {
    SourceFile squidFile = FileVisitor.createSourceFile(null, "Squid.java");
    assertEquals("Squid.java", squidFile.getKey());
  }
}
