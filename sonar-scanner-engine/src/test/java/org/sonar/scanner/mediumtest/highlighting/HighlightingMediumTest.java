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
package org.sonar.scanner.mediumtest.highlighting;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;

public class HighlightingMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  public void computeSyntaxHighlightingOnTempProject() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xoohighlightingFile = new File(srcDir, "sample.xoo.highlighting");
    FileUtils.write(xooFile, "Sample xoo\ncontent plop");
    FileUtils.write(xoohighlightingFile, "0:10:s\n11:18:k");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.highlightingTypeFor(file, 1, 0)).containsExactly(TypeOfText.STRING);
    assertThat(result.highlightingTypeFor(file, 1, 9)).containsExactly(TypeOfText.STRING);
    assertThat(result.highlightingTypeFor(file, 2, 0)).containsExactly(TypeOfText.KEYWORD);
    assertThat(result.highlightingTypeFor(file, 2, 8)).isEmpty();
  }

  @Test
  public void saveTwice() throws IOException {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent plop");

    exception.expect(UnsupportedOperationException.class);
    exception.expectMessage("Trying to save highlighting twice for the same file is not supported");
    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.it.savedatatwice", "true")
        .build())
      .execute();

  }

  @Test
  public void computeInvalidOffsets() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xoohighlightingFile = new File(srcDir, "sample.xoo.highlighting");
    FileUtils.write(xooFile, "Sample xoo\ncontent plop");
    FileUtils.write(xoohighlightingFile, "0:10:s\n18:18:k");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Error processing line 2");
    exception.expectCause(new TypeSafeMatcher<IllegalArgumentException>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Invalid cause");
      }

      @Override
      protected boolean matchesSafely(IllegalArgumentException e) {
        return e.getMessage().contains("Unable to highlight file");
      }
    });

    tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .execute();
  }

}
