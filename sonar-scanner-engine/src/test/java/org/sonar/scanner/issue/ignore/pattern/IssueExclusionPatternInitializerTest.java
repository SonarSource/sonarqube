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
package org.sonar.scanner.issue.ignore.pattern;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.IssueExclusionProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueExclusionPatternInitializerTest {
  private IssueExclusionPatternInitializer patternsInitializer;
  private MapSettings settings;

  @Before
  public void init() {
    settings = new MapSettings(new PropertyDefinitions(IssueExclusionProperties.all()));
  }

  @Test
  public void testNoConfiguration() {
    patternsInitializer = new IssueExclusionPatternInitializer(settings.asConfig());
    assertThat(patternsInitializer.hasConfiguredPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldLogInvalidResourceKey() {
    settings.setProperty("sonar.issue.ignore" + ".multicriteria", "1");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "resourceKey", "");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "ruleKey", "*");
    patternsInitializer = new IssueExclusionPatternInitializer(settings.asConfig());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldLogInvalidRuleKey() {
    settings.setProperty("sonar.issue.ignore" + ".multicriteria", "1");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "resourceKey", "*");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "ruleKey", "");
    patternsInitializer = new IssueExclusionPatternInitializer(settings.asConfig());
  }

  @Test
  public void shouldReturnBlockPattern() {
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY, "1,2,3");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionProperties.BEGIN_BLOCK_REGEXP, "// SONAR-OFF");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionProperties.END_BLOCK_REGEXP, "// SONAR-ON");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".2." + IssueExclusionProperties.BEGIN_BLOCK_REGEXP, "// FOO-OFF");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".2." + IssueExclusionProperties.END_BLOCK_REGEXP, "// FOO-ON");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".3." + IssueExclusionProperties.BEGIN_BLOCK_REGEXP, "// IGNORE-TO-EOF");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".3." + IssueExclusionProperties.END_BLOCK_REGEXP, "");
    patternsInitializer = new IssueExclusionPatternInitializer(settings.asConfig());

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasFileContentPattern()).isTrue();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(3);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(0);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldLogInvalidStartBlockPattern() {
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY, "1");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionProperties.BEGIN_BLOCK_REGEXP, "");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionProperties.END_BLOCK_REGEXP, "// SONAR-ON");
    patternsInitializer = new IssueExclusionPatternInitializer(settings.asConfig());
  }

  @Test
  public void shouldReturnAllFilePattern() {
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY, "1,2");
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY + ".1." + IssueExclusionProperties.FILE_REGEXP, "@SONAR-IGNORE-ALL");
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY + ".2." + IssueExclusionProperties.FILE_REGEXP, "//FOO-IGNORE-ALL");
    patternsInitializer = new IssueExclusionPatternInitializer(settings.asConfig());

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasFileContentPattern()).isTrue();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(2);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldLogInvalidAllFilePattern() {
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY, "1");
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY + ".1." + IssueExclusionProperties.FILE_REGEXP, "");
    patternsInitializer = new IssueExclusionPatternInitializer(settings.asConfig());
  }
}
