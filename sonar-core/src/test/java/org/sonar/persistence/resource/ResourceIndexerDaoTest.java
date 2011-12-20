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
package org.sonar.persistence.resource;

import org.junit.Before;
import org.junit.Test;
import org.sonar.persistence.DaoTestCase;

import java.util.Arrays;

public class ResourceIndexerDaoTest extends DaoTestCase {

  private static ResourceIndexerDao dao;

  @Before
  public void createDao() {
    dao = new ResourceIndexerDao(getMyBatis());
  }

  @Test
  public void shouldIndexSingleResource() {
    setupData("shouldIndexSingleResource");

    dao.index("ZipUtils", 10, 8);

    checkTables("shouldIndexSingleResource", "resource_index");
  }

  @Test
  public void shouldIndexAllResources() {
    setupData("shouldIndexAllResources");

    dao.index(ResourceIndexerFilter.create());

    checkTables("shouldIndexAllResources", "resource_index");
  }

  @Test
  public void shouldIndexMultiModulesProject() {
    setupData("shouldIndexMultiModulesProject");

    dao.index(ResourceIndexerFilter.create());

    checkTables("shouldIndexMultiModulesProject", "resource_index");
  }

  @Test
  public void shouldReindexProjectAfterRenaming() {
    setupData("shouldReindexProjectAfterRenaming");

    dao.index(ResourceIndexerFilter.create());

    checkTables("shouldReindexProjectAfterRenaming", "resource_index");
  }

  @Test
  public void shouldDeleteIndexes() {
    setupData("shouldDeleteIndexes");

    dao.delete(Arrays.asList(3, 4, 5, 6));

    checkTables("shouldDeleteIndexes", "resource_index");
  }
}
