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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

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

    assertThat(resources.size(), Is.is(1));
    assertThat(resources.get(0).getId(), is(2L));
  }

  @Test
  public void testDescendantProjects_id_not_found() {
    setupData("fixture");

    assertThat(dao.getDescendantProjects(33333L).size(), Is.is(0));
  }

  @Test
  public void getResource() {
    setupData("fixture");

    ResourceDto resource = dao.getResource(1L);
    assertThat(resource.getName(), Is.is("Struts"));
    assertThat(resource.getLongName(), Is.is("Apache Struts"));
    assertThat(resource.getScope(), Is.is("PRJ"));
  }

  @Test
  public void getResource_not_found() {
    setupData("fixture");

    assertNull(dao.getResource(987654321L));
  }

  @Test
  public void getResources_all() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create());

    assertThat(resources.size(), Is.is(4));
  }

  @Test
  public void getResources_filter_by_qualifier() {
    setupData("fixture");

    List<ResourceDto> resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{"TRK", "BRC"}));
    assertThat(resources.size(), Is.is(2));
    assertThat(resources, hasItems(new QualifierMatcher("TRK"), new QualifierMatcher("BRC")));

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{"XXX"}));
    assertThat(resources.size(), Is.is(0));

    resources = dao.getResources(ResourceQuery.create().setQualifiers(new String[]{}));
    assertThat(resources.size(), Is.is(4));
  }

  @Test
  public void getResourceIds_all() {
    setupData("fixture");

    List<Long> ids = dao.getResourceIds(ResourceQuery.create());

    assertThat(ids.size(), Is.is(4));
  }

  @Test
  public void getResourceIds_filter_by_qualifier() {
    setupData("fixture");

    List<Long> ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{"TRK", "BRC"}));
    assertThat(ids.size(), Is.is(2));
    assertThat(ids, hasItems(1l, 2l));

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{"XXX"}));
    assertThat(ids.size(), Is.is(0));

    ids = dao.getResourceIds(ResourceQuery.create().setQualifiers(new String[]{}));
    assertThat(ids.size(), Is.is(4));
  }


  private static class QualifierMatcher extends BaseMatcher<ResourceDto> {
    private String qualifier;

    private QualifierMatcher(String qualifier) {
      this.qualifier = qualifier;
    }

    public boolean matches(Object o) {
      return ((ResourceDto) o).getQualifier().equals(qualifier);
    }

    public void describeTo(Description description) {
      description.appendText("qualifier " + qualifier);
    }
  }
}

