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
package org.sonar.core.resource;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.persistence.AbstractDaoTestCase;

public class ResourceIndexerDaoTest extends AbstractDaoTestCase {

  private static ResourceIndexerDao dao;

  @Before
  public void createDao() {
    dao = new ResourceIndexerDao(getMyBatis());
  }

  @Test
  public void shouldIndexResource() {
    setupData("shouldIndexResource");

    dao.indexResource(10, "ZipUtils", "FIL", 8);

    checkTables("shouldIndexResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldIndexProjects() {
    setupData("shouldIndexProjects");

    dao.indexProjects();

    checkTables("shouldIndexProjects", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldIndexMultiModulesProject() {
    setupData("shouldIndexMultiModulesProject");

    dao.indexProject(1);

    checkTables("shouldIndexMultiModulesProject", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReindexProjectAfterRenaming() {
    setupData("shouldReindexProjectAfterRenaming");

    dao.indexProject(1);

    checkTables("shouldReindexProjectAfterRenaming", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldIndexTwoLettersLongResources() {
    setupData("shouldIndexTwoLettersLongResource");

    dao.indexResource(10, "AB", Qualifiers.PROJECT, 3);

    checkTables("shouldIndexTwoLettersLongResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReIndexTwoLettersLongResources() {
    setupData("shouldReIndexTwoLettersLongResource");

    dao.indexResource(1, "AS", Qualifiers.PROJECT, 1);

    checkTables("shouldReIndexTwoLettersLongResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReIndexNewTwoLettersLongResource() {
    setupData("shouldReIndexNewTwoLettersLongResource");

    dao.indexResource(1, "AS", Qualifiers.PROJECT, 1);

    checkTables("shouldReIndexNewTwoLettersLongResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldReindexResource() {
    setupData("shouldReindexResource");

    dao.indexResource(1, "New Struts", Qualifiers.PROJECT, 1);

    checkTables("shouldReindexResource", new String[] {"id"}, "resource_index");
  }

  @Test
  public void shouldNotReindexUnchangedResource() {
    setupData("shouldNotReindexUnchangedResource");

    dao.indexResource(1, "Struts", Qualifiers.PROJECT, 1);

    checkTables("shouldNotReindexUnchangedResource", new String[] {"id"}, "resource_index");
  }
}
