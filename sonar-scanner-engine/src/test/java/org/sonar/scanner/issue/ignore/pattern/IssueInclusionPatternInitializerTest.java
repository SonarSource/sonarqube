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
package org.sonar.scanner.issue.ignore.pattern;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.IssueExclusionProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueInclusionPatternInitializerTest {

  private IssueInclusionPatternInitializer patternsInitializer;
  private MapSettings settings;

  @Before
  public void init() {
    settings = new MapSettings(new PropertyDefinitions(IssueExclusionProperties.all()));
    patternsInitializer = new IssueInclusionPatternInitializer(settings.asConfig());
  }

  @Test
  public void testNoConfiguration() {
    patternsInitializer.initPatterns();
    assertThat(patternsInitializer.hasConfiguredPatterns()).isFalse();
  }

  @Test
  public void shouldHavePatternsBasedOnMulticriteriaPattern() {
    settings.setProperty("sonar.issue.enforce" + ".multicriteria", "1,2");
    settings.setProperty("sonar.issue.enforce" + ".multicriteria" + ".1." + "resourceKey", "org/foo/Bar.java");
    settings.setProperty("sonar.issue.enforce" + ".multicriteria" + ".1." + "ruleKey", "*");
    settings.setProperty("sonar.issue.enforce" + ".multicriteria" + ".2." + "resourceKey", "org/foo/Hello.java");
    settings.setProperty("sonar.issue.enforce" + ".multicriteria" + ".2." + "ruleKey", "checkstyle:MagicNumber");
    patternsInitializer.initPatterns();

    assertThat(patternsInitializer.hasConfiguredPatterns()).isTrue();
    assertThat(patternsInitializer.hasMulticriteriaPatterns()).isTrue();
    assertThat(patternsInitializer.getMulticriteriaPatterns().size()).isEqualTo(2);
  }

}
