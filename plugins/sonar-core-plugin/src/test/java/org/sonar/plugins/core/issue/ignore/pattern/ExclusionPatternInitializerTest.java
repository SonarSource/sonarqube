/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.plugins.core.issue.ignore.pattern;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.core.issue.ignore.IgnoreIssuesConfiguration;

import static org.fest.assertions.Assertions.assertThat;

public class ExclusionPatternInitializerTest {

  private ExclusionPatternInitializer patternsInitializer;

  private Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(IgnoreIssuesConfiguration.getPropertyDefinitions()));
    patternsInitializer = new ExclusionPatternInitializer(settings);
  }

  @Test
  public void testNoConfiguration() {
    patternsInitializer.initPatterns();
    assertThat(patternsInitializer.hasConfiguredPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
  }

  @Test
  public void shouldHavePatternsBasedOnMulticriteriaPattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY, "1,2");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "org/foo/Bar.java");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".2." + IgnoreIssuesConfiguration.RESOURCE_KEY, "org/foo/Hello.java");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".2." + IgnoreIssuesConfiguration.RULE_KEY, "checkstyle:MagicNumber");
    patternsInitializer.initPatterns();

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasFileContentPattern()).isFalse();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isTrue();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(2);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(0);

    patternsInitializer.initializePatternsForPath("org/foo/Bar.java", "org.foo.Bar");
    patternsInitializer.initializePatternsForPath("org/foo/Baz.java", "org.foo.Baz");
    patternsInitializer.initializePatternsForPath("org/foo/Hello.java", "org.foo.Hello");

    assertThat(patternsInitializer.getPatternMatcher().getPatternsForComponent("org.foo.Bar")).hasSize(1);
    assertThat(patternsInitializer.getPatternMatcher().getPatternsForComponent("org.foo.Baz")).hasSize(0);
    assertThat(patternsInitializer.getPatternMatcher().getPatternsForComponent("org.foo.Hello")).hasSize(1);

  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidResourceKey() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY, "1");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "*");
    patternsInitializer.initPatterns();
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidRuleKey() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY, "1");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "");
    patternsInitializer.initPatterns();
  }

  @Test
  public void shouldReturnBlockPattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY, "1,2,3");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".1." + IgnoreIssuesConfiguration.BEGIN_BLOCK_REGEXP, "// SONAR-OFF");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".1." + IgnoreIssuesConfiguration.END_BLOCK_REGEXP, "// SONAR-ON");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".2." + IgnoreIssuesConfiguration.BEGIN_BLOCK_REGEXP, "// FOO-OFF");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".2." + IgnoreIssuesConfiguration.END_BLOCK_REGEXP, "// FOO-ON");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".3." + IgnoreIssuesConfiguration.BEGIN_BLOCK_REGEXP, "// IGNORE-TO-EOF");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".3." + IgnoreIssuesConfiguration.END_BLOCK_REGEXP, "");
    patternsInitializer.loadFileContentPatterns();

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasFileContentPattern()).isTrue();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(3);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(0);
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidStartBlockPattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY, "1");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".1." + IgnoreIssuesConfiguration.BEGIN_BLOCK_REGEXP, "");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + ".1." + IgnoreIssuesConfiguration.END_BLOCK_REGEXP, "// SONAR-ON");
    patternsInitializer.loadFileContentPatterns();
  }

  @Test
  public void shouldReturnAllFilePattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY, "1,2");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY + ".1." + IgnoreIssuesConfiguration.FILE_REGEXP, "@SONAR-IGNORE-ALL");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY + ".2." + IgnoreIssuesConfiguration.FILE_REGEXP, "//FOO-IGNORE-ALL");
    patternsInitializer.loadFileContentPatterns();

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasFileContentPattern()).isTrue();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(2);
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidAllFilePattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY, "1");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY + ".1." + IgnoreIssuesConfiguration.FILE_REGEXP, "");
    patternsInitializer.loadFileContentPatterns();
  }
}
