/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.core.config.IssueExclusionProperties;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueExclusionPatternInitializerTest {

  private IssueExclusionPatternInitializer patternsInitializer;

  private Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(IssueExclusionProperties.all()));
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
    settings.setProperty("sonar.issue.ignore" + ".multicriteria", "1,2");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "resourceKey", "org/foo/Bar.java");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "ruleKey", "*");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".2." + "resourceKey", "org/foo/Hello.java");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".2." + "ruleKey", "checkstyle:MagicNumber");
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
    settings.setProperty("sonar.issue.ignore" + ".multicriteria", "1");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "resourceKey", "");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "ruleKey", "*");
    patternsInitializer.initPatterns();
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidRuleKey() {
    settings.setProperty("sonar.issue.ignore" + ".multicriteria", "1");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "resourceKey", "*");
    settings.setProperty("sonar.issue.ignore" + ".multicriteria" + ".1." + "ruleKey", "");
    patternsInitializer.initPatterns();
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
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY, "1");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionProperties.BEGIN_BLOCK_REGEXP, "");
    settings.setProperty(IssueExclusionProperties.PATTERNS_BLOCK_KEY + ".1." + IssueExclusionProperties.END_BLOCK_REGEXP, "// SONAR-ON");
    patternsInitializer.loadFileContentPatterns();
  }

  @Test
  public void shouldReturnAllFilePattern() {
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY, "1,2");
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY + ".1." + IssueExclusionProperties.FILE_REGEXP, "@SONAR-IGNORE-ALL");
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY + ".2." + IssueExclusionProperties.FILE_REGEXP, "//FOO-IGNORE-ALL");
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
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY, "1");
    settings.setProperty(IssueExclusionProperties.PATTERNS_ALLFILE_KEY + ".1." + IssueExclusionProperties.FILE_REGEXP, "");
    patternsInitializer.loadFileContentPatterns();
  }
}
