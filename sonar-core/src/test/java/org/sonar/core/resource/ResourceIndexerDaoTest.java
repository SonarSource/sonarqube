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
package org.sonar.core.resource;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.persistence.DaoTestCase;

public class ResourceIndexerDaoTest extends DaoTestCase {

  private static ResourceIndexerDao dao;

  @Before
  public void createDao() {
    dao = new ResourceIndexerDao(getMyBatis());
  }

  @Test
  public void shouldIndexResource() {
    setupData("shouldIndexResource");

    dao.indexResource(10, "ZipUtils", "FIL", 8);

    checkTables("shouldIndexResource", "resource_index");
  }

  @Test
  public void shouldIndexProjects() {
    setupData("shouldIndexProjects");

    dao.indexProjects();

    checkTables("shouldIndexProjects", "resource_index");
  }

  @Test
  public void shouldIndexMultiModulesProject() {
    setupData("shouldIndexMultiModulesProject");

    dao.indexProject(1);

    checkTables("shouldIndexMultiModulesProject", "resource_index");
  }

  @Test
  public void shouldReindexProjectAfterRenaming() {
    setupData("shouldReindexProjectAfterRenaming");

    dao.indexProject(1);

    checkTables("shouldReindexProjectAfterRenaming", "resource_index");
  }

  @Test
  public void shouldNotIndexPackage() {
    setupData("empty");

    dao.indexResource(10, "org.codehaus.sonar", Qualifiers.PACKAGE, 3);

    checkTables("empty", "resource_index");
  }

  @Test
  public void shouldNotIndexWhenTooShortName() {
    setupData("empty");

    dao.indexResource(10, "AB", Qualifiers.FILE, 3);

    checkTables("empty", "resource_index");
  }

  @Test
  public void shouldReindexResource() {
    setupData("shouldReindexResource");

    dao.indexResource(1, "New Struts", Qualifiers.PROJECT, 1);

    checkTables("shouldReindexResource", "resource_index");
  }

  @Test
  public void shouldNotReindexUnchangedResource() {
    setupData("shouldNotReindexUnchangedResource");

    dao.indexResource(1, "Struts", Qualifiers.PROJECT, 1);

    checkTables("shouldNotReindexUnchangedResource", "resource_index");
  }
}
