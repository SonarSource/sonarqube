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
import org.sonar.plugins.core.issue.ignore.IgnoreIssuesConfiguration;

import static org.fest.assertions.Assertions.assertThat;

public class InclusionPatternInitializerTest {

  private InclusionPatternInitializer patternsInitializer;

  private Settings settings;

  @Before
  public void init() {
    settings = new Settings(new PropertyDefinitions(IgnoreIssuesConfiguration.getPropertyDefinitions()));
    patternsInitializer = new InclusionPatternInitializer(settings);
  }

  @Test
  public void testNoConfiguration() {
    patternsInitializer.initPatterns();
    assertThat(patternsInitializer.hasConfiguredPatterns()).isFalse();
  }

  @Test
  public void shouldHavePatternsBasedOnMulticriteriaPattern() {
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_INCLUSION_KEY, "1,2");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_INCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RESOURCE_KEY, "org/foo/Bar.java");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_INCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.RULE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_INCLUSION_KEY + ".1." + IgnoreIssuesConfiguration.LINE_RANGE_KEY, "*");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_INCLUSION_KEY + ".2." + IgnoreIssuesConfiguration.RESOURCE_KEY, "org/foo/Hello.java");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_INCLUSION_KEY + ".2." + IgnoreIssuesConfiguration.RULE_KEY, "checkstyle:MagicNumber");
    settings.setProperty(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_INCLUSION_KEY + ".2." + IgnoreIssuesConfiguration.LINE_RANGE_KEY, "[15-200]");
    patternsInitializer.initPatterns();

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isTrue();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(2);

    patternsInitializer.initializePatternsForPath("org/foo/Bar.java", "org.foo.Bar");
    patternsInitializer.initializePatternsForPath("org/foo/Baz.java", "org.foo.Baz");
    patternsInitializer.initializePatternsForPath("org/foo/Hello.java", "org.foo.Hello");

    assertThat(patternsInitializer.getPathForComponent("org.foo.Bar")).isEqualTo("org/foo/Bar.java");
    assertThat(patternsInitializer.getPathForComponent("org.foo.Baz")).isEqualTo("org/foo/Baz.java");
    assertThat(patternsInitializer.getPathForComponent("org.foo.Hello")).isEqualTo("org/foo/Hello.java");
  }

}
