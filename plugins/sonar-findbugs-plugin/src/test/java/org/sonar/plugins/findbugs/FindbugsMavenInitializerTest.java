/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.findbugs;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MavenTestUtils;

public class FindbugsMavenInitializerTest {

  private Project project;
  private FindbugsMavenInitializer initializer;

  @Before
  public void setUp() {
    project = mock(Project.class);
    initializer = new FindbugsMavenInitializer();
  }

  @Test
  public void doNotSetExcludesFiltersIfAlreadyConfigured() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.containsKey(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY)).thenReturn(true);
    when(project.getConfiguration()).thenReturn(configuration);
    initializer.execute(project);
    verify(configuration, never()).setProperty(eq(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY), anyString());
  }

  @Test
  public void shouldGetExcludesFiltersFromPom() {
    Project project = MavenTestUtils.loadProjectFromPom(getClass(), "pom.xml");
    initializer.execute(project);
    assertThat(project.getConfiguration().getString(FindbugsConstants.EXCLUDES_FILTERS_PROPERTY), is("foo.xml"));
  }

}
