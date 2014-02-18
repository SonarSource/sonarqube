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

package org.sonar.batch.issue.ignore.pattern;

import org.sonar.batch.issue.ignore.IssueExclusionsConfiguration;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import static org.fest.assertions.Assertions.assertThat;

public class IssueExclusionPatternInitializerTest {

  private IssueExclusionPatternInitializer patternsInitializer;

  private Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(IssueExclusionsConfiguration.getPropertyDefinitions()));
    patternsInitializer = new IssueExclusionPatternInitializer(settings);
  }

  @Test
  public void testNoConfiguration() {
    patternsInitializer.initPatterns();
    assertThat(patternsInitializer.hasConfiguredPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
  }

  @Test
  public void shouldHavePatternsBasedOnMulticriteriaPattern() {
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY, "1,2");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IssueExclusionsConfiguration.RESOURCE_KEY, "org/foo/Bar.java");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IssueExclusionsConfiguration.RULE_KEY, "*");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".2." + IssueExclusionsConfiguration.RESOURCE_KEY, "org/foo/Hello.java");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".2." + IssueExclusionsConfiguration.RULE_KEY, "checkstyle:MagicNumber");
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
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY, "1");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IssueExclusionsConfiguration.RESOURCE_KEY, "");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IssueExclusionsConfiguration.RULE_KEY, "*");
    patternsInitializer.initPatterns();
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidRuleKey() {
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY, "1");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IssueExclusionsConfiguration.RESOURCE_KEY, "*");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_MULTICRITERIA_EXCLUSION_KEY + ".1." + IssueExclusionsConfiguration.RULE_KEY, "");
    patternsInitializer.initPatterns();
  }

  @Test
  public void shouldReturnBlockPattern() {
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY, "1,2,3");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionsConfiguration.BEGIN_BLOCK_REGEXP, "// SONAR-OFF");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionsConfiguration.END_BLOCK_REGEXP, "// SONAR-ON");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".2." + IssueExclusionsConfiguration.BEGIN_BLOCK_REGEXP, "// FOO-OFF");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".2." + IssueExclusionsConfiguration.END_BLOCK_REGEXP, "// FOO-ON");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".3." + IssueExclusionsConfiguration.BEGIN_BLOCK_REGEXP, "// IGNORE-TO-EOF");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".3." + IssueExclusionsConfiguration.END_BLOCK_REGEXP, "");
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
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY, "1");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionsConfiguration.BEGIN_BLOCK_REGEXP, "");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionsConfiguration.END_BLOCK_REGEXP, "// SONAR-ON");
    patternsInitializer.loadFileContentPatterns();
  }

  @Test
  public void shouldReturnAllFilePattern() {
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_ALLFILE_KEY, "1,2");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_ALLFILE_KEY + ".1." + IssueExclusionsConfiguration.FILE_REGEXP, "@SONAR-IGNORE-ALL");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_ALLFILE_KEY + ".2." + IssueExclusionsConfiguration.FILE_REGEXP, "//FOO-IGNORE-ALL");
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
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_ALLFILE_KEY, "1");
    settings.setProperty(IssueExclusionsConfiguration.PATTERNS_ALLFILE_KEY + ".1." + IssueExclusionsConfiguration.FILE_REGEXP, "");
    patternsInitializer.loadFileContentPatterns();
  }
}
