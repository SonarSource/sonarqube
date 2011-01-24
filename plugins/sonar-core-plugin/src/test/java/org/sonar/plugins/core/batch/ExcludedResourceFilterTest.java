/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExcludedResourceFilterTest {

  @Test
  public void doNotFailIfNoPatterns() {
    ExcludedResourceFilter filter = new ExcludedResourceFilter((String[]) null);
    assertThat(filter.isIgnored(mock(Resource.class)), is(false));
  }

  @Test
  public void noPatternsMatch() {
    ExcludedResourceFilter filter = new ExcludedResourceFilter(new String[]{"**/foo/*.java", "**/bar/*"});
    assertThat(filter.isIgnored(mock(Resource.class)), is(false));
  }

  /**
   * See SONAR-1115 Exclusion patterns do not apply to unit tests.
   */
  @Test
  public void ignoreResourceIfMatchesPattern() {
    ExcludedResourceFilter filter = new ExcludedResourceFilter(new String[]{"**/foo/*.java", "**/bar/*"});

    Resource resource = mock(Resource.class);
    when(resource.matchFilePattern("**/bar/*")).thenReturn(true);

    assertThat(filter.isIgnored(resource), is(true));
  }

  @Test
  public void doNotExcludeUnitTestFiles() {
    ExcludedResourceFilter filter = new ExcludedResourceFilter(new String[]{"**/foo/*.java", "**/bar/*"});

    Resource unitTest = mock(Resource.class);
    when(unitTest.getQualifier()).thenReturn(Qualifiers.UNIT_TEST_FILE);

    // match exclusion pattern
    when(unitTest.matchFilePattern("**/bar/*")).thenReturn(true);

    assertThat(filter.isIgnored(unitTest), is(false));
  }
}
