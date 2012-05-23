/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import org.sonar.core.persistence.DaoTestCase;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ResourceDaoTest extends DaoTestCase {

  private ResourceDao dao;

  @Before
  public void createDao() {
    dao = new ResourceDao(getMyBatis());
  }

  @Test
  public void testDescendantProjects_do_not_include_self() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getDescendantProjects(1L);

    assertThat(resources).onProperty("id").containsOnly(2L);
  }

  @Test
  public void testDescendantProjects_id_not_found() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getDescendantProjects(33333L);

    assertThat(resources).isEmpty();
  }

  @Test
  public void getResource() {
    setupData("fixture");

    ResourceDto resource = dao.getResource(1L);

    assertThat(resource.getName()).isEqualTo("Struts");
    assertThat(resource.getLongName()).isEqualTo("Apache Struts");
    assertThat(resource.getScope()).isEqualTo("PRJ");
  }

  @Test
  public void getResource_not_found() {
    setupData("fixture");

    assertThat(dao.getResource(987654321L)).isNull();
  }

  @Test
  public void getResources_all() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create());

    assertThat(resources).hasSize(4);
  }

  @Test
  public void getResources_filter_by_qualifier() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{"TRK", "BRC"}));
    assertThat(resources).onProperty("qualifier").containsOnly("TRK", "BRC");

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{"XXX"}));
    assertThat(resources).isEmpty();

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{}));
    assertThat(resources).hasSize(4);
  }

  @Test
  public void getResourceIds_all() {
    setupData("fixture");

    List<Long> ids = dao.getResourceIds(ResourceQuery.create());

    assertThat(ids).hasSize(4);
  }

  @Test
  public void getResourceIds_filter_by_qualifier() {
    setupData("fixture");

    List<Long> ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{"TRK", "BRC"}));
    assertThat(ids).containsOnly(1L, 2L);

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{"XXX"}));
    assertThat(ids).isEmpty();

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{}));
    assertThat(ids).hasSize(4);
  }
}

