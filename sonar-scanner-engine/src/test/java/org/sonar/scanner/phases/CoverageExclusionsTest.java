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
package org.sonar.scanner.phases;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.ExclusionProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class CoverageExclusionsTest {

  private MapSettings settings;
  private CoverageExclusions coverageExclusions;

  @Before
  public void prepare() {
    settings = new MapSettings(new PropertyDefinitions(ExclusionProperties.all()));
  }

  @Test
  public void shouldExcludeFileBasedOnPattern() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/org/polop/File.php").build();
    settings.setProperty("sonar.coverage.exclusions", "src/org/polop/*");
    coverageExclusions = new CoverageExclusions(settings.asConfig());
    assertThat(coverageExclusions.isExcluded(file)).isTrue();
  }

  @Test
  public void shouldNotExcludeFileBasedOnPattern() {
    DefaultInputFile file = new TestInputFileBuilder("foo", "src/org/polop/File.php").build();
    settings.setProperty("sonar.coverage.exclusions", "src/org/other/*");
    coverageExclusions = new CoverageExclusions(settings.asConfig());
    assertThat(coverageExclusions.isExcluded(file)).isFalse();
  }
}
