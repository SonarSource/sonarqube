/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.batch.maven;

import com.google.common.base.Charsets;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.test.MavenTestUtils;

import java.nio.charset.Charset;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MavenUtilsTest {
  private MavenProject pom;

  @Before
  public void setUp() {
    pom = MavenTestUtils.loadPom("/org/sonar/api/batch/maven/MavenPom.xml");
  }

  @Test
  public void getJavaVersion() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "getJavaVersion.xml");
    assertThat(MavenUtils.getJavaVersion(pom), is("1.4"));
  }

  @Test
  public void getJavaVersionFromPluginManagement() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "getJavaVersionFromPluginManagement.xml");
    assertThat(MavenUtils.getJavaVersion(pom), is("1.4"));
  }

  @Test
  public void testDefaultSourceEncoding() {
    assertEquals(MavenUtils.getSourceCharset(pom), Charset.defaultCharset());
  }

  @Test
  public void testSourceEncoding() {
    MavenProject pom = MavenTestUtils.loadPom("/org/sonar/api/batch/maven/MavenPomWithSourceEncoding.xml");
    assertEquals(MavenUtils.getSourceCharset(pom), Charsets.UTF_16);
  }
}
