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
package org.sonar.server.startup;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenerateBootstrapIndexTest {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void shouldDetermineListOfResources() {
    ServletContext servletContext = mock(ServletContext.class);
    Set<String> libs = Sets.newHashSet();
    libs.add("/WEB-INF/lib/sonar-core-2.6.jar");
    libs.add("/WEB-INF/lib/treemap.rb");
    libs.add("/WEB-INF/lib/directory/");
    when(servletContext.getResourcePaths(anyString())).thenReturn(libs);

    assertThat(GenerateBootstrapIndex.getLibs(servletContext)).hasSize(1);
    assertThat(GenerateBootstrapIndex.getLibs(servletContext).get(0)).isEqualTo("sonar-core-2.6.jar");
  }

  @Test
  public void shouldIgnore() {
    assertThat(GenerateBootstrapIndex.isIgnored("sonar-batch-2.6-SNAPSHOT.jar")).isFalse();
    assertThat(GenerateBootstrapIndex.isIgnored("mysql-connector-java-5.1.13.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("postgresql-9.0-801.jdbc3.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jtds-1.2.4.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jfreechart-1.0.9.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("eastwood-1.1.0.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jruby-complete-1.5.6.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jruby-rack-1.0.5.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("elasticsearch-0.90.6.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("lucene-core-4.5.1.jar")).isTrue();
  }

}
