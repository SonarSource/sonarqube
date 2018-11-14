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
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.ExclusionProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class ModuleCoverageExclusionsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private MapSettings settings;
  private ModuleCoverageExclusions coverageExclusions;
  private File baseDir;

  @Before
  public void prepare() throws Exception {
    settings = new MapSettings(new PropertyDefinitions(ExclusionProperties.all()));
    baseDir = temp.newFolder();
  }

  @Test
  public void shouldExcludeFileBasedOnPattern() {
    DefaultInputFile file = TestInputFileBuilder.create("foo", new File(baseDir, "moduleA"), new File(baseDir, "moduleA/src/org/polop/File.php"))
      .setProjectBaseDir(baseDir.toPath())
      .build();
    settings.setProperty("sonar.coverage.exclusions", "src/org/polop/*");
    coverageExclusions = new ModuleCoverageExclusions(settings.asConfig());
    assertThat(coverageExclusions.isExcluded(file)).isTrue();
  }

  @Test
  public void shouldNotExcludeFileBasedOnPattern() {
    DefaultInputFile file = TestInputFileBuilder.create("foo", new File(baseDir, "moduleA"), new File(baseDir, "moduleA/src/org/polop/File.php"))
      .setProjectBaseDir(baseDir.toPath())
      .build();
    settings.setProperty("sonar.coverage.exclusions", "src/org/other/*");
    coverageExclusions = new ModuleCoverageExclusions(settings.asConfig());
    assertThat(coverageExclusions.isExcluded(file)).isFalse();
  }
}
