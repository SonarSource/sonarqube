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
package org.sonar.api.resources;

import org.junit.Test;
import org.sonar.api.test.MavenTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class ProjectTest {
  @Test
  public void equalsProject() {
    Project project1 = MavenTestUtils.loadProjectFromPom(getClass(), "equalsProject/pom.xml");
    Project project2 = MavenTestUtils.loadProjectFromPom(getClass(), "equalsProject/pom.xml");
    assertEquals(project1, project2);
    assertFalse("foo:bar".equals(project1));
    assertEquals(project1.hashCode(), project2.hashCode());
  }

  @Test
  public void createFromMavenIds() {
    Project project = Project.createFromMavenIds("my", "artifact");
    assertThat(project.getKey(), is("my:artifact"));
  }
}
