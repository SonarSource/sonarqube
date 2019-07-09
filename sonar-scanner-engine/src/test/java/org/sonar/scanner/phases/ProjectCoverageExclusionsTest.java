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
package org.sonar.scanner.phases;

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.scan.ProjectConfiguration;
import org.sonar.scanner.scan.filesystem.ProjectCoverageAndDuplicationExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectCoverageExclusionsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ProjectCoverageAndDuplicationExclusions underTest;
  private File baseDir;

  @Before
  public void prepare() throws Exception {
    baseDir = temp.newFolder();
  }

  @Test
  public void shouldExcludeFileCoverageBasedOnPattern() {
    DefaultInputFile file = TestInputFileBuilder.create("foo", new File(baseDir, "moduleA"), new File(baseDir, "moduleA/src/org/polop/File.php"))
      .setProjectBaseDir(baseDir.toPath())
      .build();
    underTest = new ProjectCoverageAndDuplicationExclusions(mockConfig("moduleA/src/org/polop/*", ""));
    assertThat(underTest.isExcludedForCoverage(file)).isTrue();
    assertThat(underTest.isExcludedForDuplication(file)).isFalse();
  }

  @Test
  public void shouldNotExcludeFileCoverageBasedOnPattern() {
    DefaultInputFile file = TestInputFileBuilder.create("foo", new File(baseDir, "moduleA"), new File(baseDir, "moduleA/src/org/polop/File.php"))
      .setProjectBaseDir(baseDir.toPath())
      .build();
    underTest = new ProjectCoverageAndDuplicationExclusions(mockConfig("moduleA/src/org/other/*", ""));
    assertThat(underTest.isExcludedForCoverage(file)).isFalse();
    assertThat(underTest.isExcludedForDuplication(file)).isFalse();
  }

  @Test
  public void shouldExcludeFileDuplicationBasedOnPattern() {
    DefaultInputFile file = TestInputFileBuilder.create("foo", new File(baseDir, "moduleA"), new File(baseDir, "moduleA/src/org/polop/File.php"))
      .setProjectBaseDir(baseDir.toPath())
      .build();
    underTest = new ProjectCoverageAndDuplicationExclusions(mockConfig("", "moduleA/src/org/polop/*"));
    assertThat(underTest.isExcludedForCoverage(file)).isFalse();
    assertThat(underTest.isExcludedForDuplication(file)).isTrue();
  }

  @Test
  public void shouldNotExcludeFileDuplicationBasedOnPattern() {
    DefaultInputFile file = TestInputFileBuilder.create("foo", new File(baseDir, "moduleA"), new File(baseDir, "moduleA/src/org/polop/File.php"))
      .setProjectBaseDir(baseDir.toPath())
      .build();
    underTest = new ProjectCoverageAndDuplicationExclusions(mockConfig("", "moduleA/src/org/other/*"));
    assertThat(underTest.isExcludedForCoverage(file)).isFalse();
    assertThat(underTest.isExcludedForDuplication(file)).isFalse();
  }

  private ProjectConfiguration mockConfig(String coverageExclusions, String cpdExclusions) {
    ProjectConfiguration config = mock(ProjectConfiguration.class);
    when(config.getStringArray(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY)).thenReturn(new String[] {coverageExclusions});
    when(config.getStringArray(CoreProperties.CPD_EXCLUSIONS)).thenReturn(new String[] {cpdExclusions});
    return config;
  }

}
