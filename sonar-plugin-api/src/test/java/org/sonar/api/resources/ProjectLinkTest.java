/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.BaseModelTestCase;

public class ProjectLinkTest extends BaseModelTestCase {

  private ProjectLink link;

  @Before
  public void setUp() throws Exception {
    link = new ProjectLink();
  }

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

}
