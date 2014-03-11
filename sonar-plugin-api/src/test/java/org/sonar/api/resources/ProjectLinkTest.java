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
package org.sonar.api.resources;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProjectLinkTest {

  ProjectLink link = new ProjectLink();

  @Test
  public void testSetName() {
    link.setName(overFillString(ProjectLink.NAME_COLUMN_SIZE));
    assertAbbreviated(ProjectLink.NAME_COLUMN_SIZE, link.getName());
  }

  @Test
  public void testSetHref() {
    link.setHref(overFillString(ProjectLink.HREF_COLUMN_SIZE));
    assertAbbreviated(ProjectLink.HREF_COLUMN_SIZE, link.getHref());
  }

  @Test
  public void testConstructor() {
    link = new ProjectLink("home",
      overFillString(ProjectLink.NAME_COLUMN_SIZE),
      overFillString(ProjectLink.HREF_COLUMN_SIZE));
    assertAbbreviated(ProjectLink.NAME_COLUMN_SIZE, link.getName());
    assertAbbreviated(ProjectLink.HREF_COLUMN_SIZE, link.getHref());
  }

  private String overFillString(int maxSize) {
    StringBuilder overFilled = new StringBuilder();
    for (int i = 0; i < 50 + maxSize; i++) {
      overFilled.append("x");
    }
    return overFilled.toString();
  }

  private void assertAbbreviated(int maxSize, String value) {
    assertEquals(maxSize, value.length());
    assertEquals('.', value.charAt(maxSize - 1));
  }
}
