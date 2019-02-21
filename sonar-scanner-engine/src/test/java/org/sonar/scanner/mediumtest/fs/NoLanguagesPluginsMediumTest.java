/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.mediumtest.fs;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.mediumtest.ScannerMediumTester;

public class NoLanguagesPluginsMediumTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester();

  @Test
  public void testNoLanguagePluginsInstalled() throws Exception {
    File projectDir = copyProject("test-resources/mediumtest/xoo/sample");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("No language plugins are installed");

    tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .execute();
  }

  private File copyProject(String path) throws Exception {
    File projectDir = temp.newFolder();
    File originalProjectDir = new File(path);
    FileUtils.copyDirectory(originalProjectDir, projectDir, FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(".sonar")));
    return projectDir;
  }
}
