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
package org.sonar.scanner.mediumtest.symbol;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;

public class SymbolMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  public void computeSymbolReferencesOnTempProject() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooSymbolFile = new File(srcDir, "sample.xoo.symbol");
    FileUtils.write(xooFile, "Sample xoo\ncontent\nanother xoo");
    // Highlight xoo symbol
    FileUtils.write(xooSymbolFile, "7:10,27");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.symbolReferencesFor(file, 1, 7)).containsOnly(ScannerReport.TextRange.newBuilder().setStartLine(3).setStartOffset(8).setEndLine(3).setEndOffset(11).build());
  }

  @Test
  public void computeSymbolReferencesWithVariableLength() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooSymbolFile = new File(srcDir, "sample.xoo.symbol");
    FileUtils.write(xooFile, "Sample xoo\ncontent\nanother xoo\nyet another");
    // Highlight xoo symbol
    FileUtils.write(xooSymbolFile, "7:10,27:32");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.symbolReferencesFor(file, 1, 7)).containsOnly(ScannerReport.TextRange.newBuilder().setStartLine(3).setStartOffset(8).setEndLine(4).setEndOffset(1).build());
  }

}
