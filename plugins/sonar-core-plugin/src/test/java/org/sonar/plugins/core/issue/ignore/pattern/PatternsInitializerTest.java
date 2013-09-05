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

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.plugins.core.issue.ignore.Constants;
import org.sonar.plugins.core.issue.ignore.IgnoreIssuesPlugin;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class PatternsInitializerTest {

  private PatternsInitializer patternsInitializer;

  private Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(new IgnoreIssuesPlugin()));
    patternsInitializer = new PatternsInitializer(settings);
  }

  @Test
  public void testNoConfiguration() {
    patternsInitializer.initPatterns();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
  }

  @Test
  public void shouldReturnExtraPatternForResource() {
    String file = "foo";
    patternsInitializer.addPatternToExcludeResource(file);

    Pattern extraPattern = patternsInitializer.getExtraPattern(file);
    assertThat(extraPattern.matchResource(file)).isTrue();
    assertThat(extraPattern.isCheckLines()).isFalse();
  }

  @Test
  public void shouldReturnExtraPatternForLinesOfResource() {
    String file = "foo";
    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(25, 28));
    patternsInitializer.addPatternToExcludeLines(file, lineRanges);

    Pattern extraPattern = patternsInitializer.getExtraPattern(file);
    assertThat(extraPattern.matchResource(file)).isTrue();
    assertThat(extraPattern.getAllLines()).isEqualTo(Sets.newHashSet(25, 26, 27, 28));
  }

  @Test
  public void shouldReturnMulticriteriaPattern() {
    settings.setProperty(Constants.PATTERNS_MULTICRITERIA_KEY, "1,2");
    settings.setProperty(Constants.PATTERNS_MULTICRITERIA_KEY + ".1." + Constants.RESOURCE_KEY, "org.foo.Bar");
    settings.setProperty(Constants.PATTERNS_MULTICRITERIA_KEY + ".1." + Constants.RULE_KEY, "*");
    settings.setProperty(Constants.PATTERNS_MULTICRITERIA_KEY + ".1." + Constants.RESOURCE_KEY, "*");
    settings.setProperty(Constants.PATTERNS_MULTICRITERIA_KEY + ".2." + Constants.LINE_RANGE_KEY, "org.foo.Hello");
    settings.setProperty(Constants.PATTERNS_MULTICRITERIA_KEY + ".2." + Constants.RULE_KEY, "checkstyle:MagicNumber");
    settings.setProperty(Constants.PATTERNS_MULTICRITERIA_KEY + ".2." + Constants.LINE_RANGE_KEY, "[15-200]");
    patternsInitializer.initPatterns();

    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(2);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(0);
  }

  @Test
  public void shouldReturnBlockPattern() {
    settings.setProperty(Constants.PATTERNS_BLOCK_KEY, "1,2");
    settings.setProperty(Constants.PATTERNS_BLOCK_KEY + ".1." + Constants.BEGIN_BLOCK_REGEXP, "// SONAR-OFF");
    settings.setProperty(Constants.PATTERNS_BLOCK_KEY + ".1." + Constants.END_BLOCK_REGEXP, "// SONAR-ON");
    settings.setProperty(Constants.PATTERNS_BLOCK_KEY + ".2." + Constants.BEGIN_BLOCK_REGEXP, "// FOO-OFF");
    settings.setProperty(Constants.PATTERNS_BLOCK_KEY + ".2." + Constants.END_BLOCK_REGEXP, "// FOO-ON");
    patternsInitializer.initPatterns();

    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(2);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(0);
  }

  @Test
  public void shouldReturnAllFilePattern() {
    settings.setProperty(Constants.PATTERNS_ALLFILE_KEY, "1,2");
    settings.setProperty(Constants.PATTERNS_ALLFILE_KEY + ".1." + Constants.FILE_REGEXP, "@SONAR-IGNORE-ALL");
    settings.setProperty(Constants.PATTERNS_ALLFILE_KEY + ".2." + Constants.FILE_REGEXP, "//FOO-IGNORE-ALL");
    patternsInitializer.initPatterns();

    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getBlockPatterns().size()).isEqualTo(0);
    assertThat(patternsInitializer.getAllFilePatterns().size()).isEqualTo(2);
  }
}
