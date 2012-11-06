/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.batch;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExcludedResourceFilterTest {
  Resource resource = mock(Resource.class);

  @Test
  public void doNotFailIfNoPatterns() {
    Project project = new Project("foo").setConfiguration(configWithExclusions());

    ExcludedResourceFilter filter = new ExcludedResourceFilter(project);

    assertThat(filter.isIgnored(resource)).isFalse();
  }

  @Test
  public void noPatternsMatch() {
    Project project = new Project("foo").setConfiguration(configWithExclusions("**/foo/*.java", "**/bar/*"));

    ExcludedResourceFilter filter = new ExcludedResourceFilter(project);

    assertThat(filter.isIgnored(resource)).isFalse();
  }

  @Test
  public void ignoreResourceIfMatchesPattern() {
    when(resource.matchFilePattern("**/bar/*")).thenReturn(true);

    Project project = new Project("foo").setConfiguration(configWithExclusions("**/foo/*.java", "**/bar/*"));
    ExcludedResourceFilter filter = new ExcludedResourceFilter(project);

    assertThat(filter.isIgnored(resource)).isTrue();
  }

  @Test
  public void ignoreTestIfMatchesPattern() {
    when(resource.getQualifier()).thenReturn(Qualifiers.UNIT_TEST_FILE);
    when(resource.matchFilePattern("**/bar/*")).thenReturn(true);

    Project project = new Project("foo").setConfiguration(configWithTestExclusions("**/foo/*.java", "**/bar/*"));
    ExcludedResourceFilter filter = new ExcludedResourceFilter(project);

    assertThat(filter.isIgnored(resource)).isTrue();
  }

  /**
   * See SONAR-1115 Source exclusion patterns do not apply to unit tests.
   */
  @Test
  public void doNotExcludeUnitTestFiles() {
    when(resource.getQualifier()).thenReturn(Qualifiers.UNIT_TEST_FILE);
    when(resource.matchFilePattern("**/bar/*")).thenReturn(true);

    Project project = new Project("foo").setConfiguration(configWithExclusions("**/foo/*.java", "**/bar/*"));
    ExcludedResourceFilter filter = new ExcludedResourceFilter(project);

    assertThat(filter.isIgnored(resource)).isFalse();
  }

  static PropertiesConfiguration configWithExclusions(String... exclusions) {
    return config(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, exclusions);
  }

  static PropertiesConfiguration configWithTestExclusions(String... exclusions) {
    return config(CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY, exclusions);
  }

  static PropertiesConfiguration config(String property, String... exclusions) {
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty(property, exclusions);
    return config;
  }
}
