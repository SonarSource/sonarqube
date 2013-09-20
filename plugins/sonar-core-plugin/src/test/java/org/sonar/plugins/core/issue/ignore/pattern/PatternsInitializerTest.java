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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.core.issue.ignore.IgnoreIssuesConfiguration;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatternsInitializerTest {

  private PatternsInitializer patternsInitializer;

  private Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(IgnoreIssuesConfiguration.getPropertyDefinitions()));
    patternsInitializer = new PatternsInitializer(settings);
  }

  @Test
  public void testNoConfiguration() {
    patternsInitializer.initPatterns();
    assertThat(patternsInitializer.hasConfiguredPatterns()).isFalse();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
  }

  @Test
  public void shouldReturnExtraPatternForResource() {
    String file = "foo";
    patternsInitializer.addPatternToExcludeResource(file);

    IssuePattern extraPattern = patternsInitializer.getPatternsForComponent(file).get(0);
    assertThat(extraPattern.matchResource(file)).isTrue();
    assertThat(extraPattern.isCheckLines()).isFalse();
  }

  @Test
  public void shouldReturnExtraPatternForLinesOfResource() {
    String file = "foo";
    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(25, 28));
    patternsInitializer.addPatternToExcludeLines(file, lineRanges);

    IssuePattern extraPattern = patternsInitializer.getPatternsForComponent(file).get(0);
    assertThat(extraPattern.matchResource(file)).isTrue();
    assertThat(extraPattern.getAllLines()).isEqualTo(Sets.newHashSet(25, 26, 27, 28));
  }

  @Test
  public void shouldReturnMulticriteriaPattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY, "1,2");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "org/foo/Bar.java");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.LINE_RANGE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".2." + IgnoreIssuesConfiguration.RESOURCE_KEY, "org/foo/Hello.java");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".2." + IgnoreIssuesConfiguration.RULE_KEY, "checkstyle:MagicNumber");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".2." + IgnoreIssuesConfiguration.LINE_RANGE_KEY, "[15-200]");
    patternsInitializer.initPatterns();

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasFileContentPattern()).isFalse();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isTrue();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(2);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(0);
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidResourceKey() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY, "1");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.LINE_RANGE_KEY, "*");
    patternsInitializer.initPatterns();
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidRuleKey() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY, "1");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.LINE_RANGE_KEY, "*");
    patternsInitializer.initPatterns();
  }

  @Test(expected = SonarException.class)
  public void shouldLogInvalidLineRange() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY, "1");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "org/foo/Bar.java");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + ".1." + IgnoreIssuesConfiguration.LINE_RANGE_KEY, "notALineRange");
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
    patternsInitializer.initPatterns();

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
    patternsInitializer.initPatterns();
  }

  @Test
  public void shouldReturnAllFilePattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY, "1,2");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY + ".1." + IgnoreIssuesConfiguration.FILE_REGEXP, "@SONAR-IGNORE-ALL");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY + ".2." + IgnoreIssuesConfiguration.FILE_REGEXP, "//FOO-IGNORE-ALL");
    patternsInitializer.initPatterns();

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
    patternsInitializer.initPatterns();
  }

  @Test
  public void shouldConfigurePatternsForComponents() {
    String componentKey = "groupId:artifactId:org.foo.Bar";
    String path = "org/foo/Bar.java";

    IssuePattern matching1, matching2, notMatching;
    matching1 = mock(IssuePattern.class);
    when(matching1.matchResource(path)).thenReturn(true);
    matching2 = mock(IssuePattern.class);
    when(matching2.matchResource(path)).thenReturn(true);
    notMatching = mock(IssuePattern.class);
    when(notMatching.matchResource(path)).thenReturn(false);

    patternsInitializer.initPatterns();
    patternsInitializer.getMulticriteriaPatterns().addAll(Lists.newArrayList(matching1, matching2, notMatching));
    patternsInitializer.configurePatternsForComponent(componentKey, path);

    assertThat(patternsInitializer.getPatternsForComponent(componentKey).size()).isEqualTo(2);
    assertThat(patternsInitializer.getPatternsForComponent("other").size()).isEqualTo(0);
  }
}
