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
package org.sonar.server.component.persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.ComponentQuery;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import static org.fest.assertions.Assertions.assertThat;

public class ComponentDaoTest extends AbstractDaoTestCase {

  DbSession session;

  ComponentDao dao;

  @Before
  public void createDao() throws Exception {
    session = getMyBatis().openSession(false);
    dao = new ComponentDao();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void get_by_key() {
    setupData("shared");

    ComponentDto result = dao.getByKey("org.struts:struts-core:src/org/struts/RequestContext.java", session);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts-core:src/org/struts/RequestContext.java");
    assertThat(result.path()).isEqualTo("src/org/struts/RequestContext.java");
    assertThat(result.name()).isEqualTo("RequestContext.java");
    assertThat(result.longName()).isEqualTo("org.struts.RequestContext");
    assertThat(result.qualifier()).isEqualTo("FIL");
    assertThat(result.subProjectId()).isEqualTo(2);
    assertThat(result.projectId()).isEqualTo(1);

    assertThat(dao.getByKey("unknown", session)).isNull();
  }

  @Test
  public void get_by_key_on_a_root_project() {
    setupData("shared");

    ComponentDto result = dao.getByKey("org.struts:struts", session);
    assertThat(result).isNotNull();
    assertThat(result.key()).isEqualTo("org.struts:struts");
    assertThat(result.path()).isNull();
    assertThat(result.name()).isEqualTo("Struts");
    assertThat(result.longName()).isEqualTo("Apache Struts");
    assertThat(result.qualifier()).isEqualTo("TRK");
    assertThat(result.subProjectId()).isNull();
    assertThat(result.projectId()).isEqualTo(1);
  }

  @Test
  public void get_by_id() {
    setupData("shared");

    assertThat(dao.getById(4L, session)).isNotNull();
    assertThat(dao.getById(111L, session)).isNull();
  }

  @Test
  public void find_by_query() {
    setupData("shared");
    assertThat(dao.findByQuery(ComponentQuery.create(), session)).hasSize(4);
    assertThat(dao.findByQuery(ComponentQuery.create().addIds(1L), session)).hasSize(1);
    assertThat(dao.findByQuery(ComponentQuery.create().addQualifiers(Qualifiers.PROJECT), session)).hasSize(1);
    assertThat(dao.findByQuery(ComponentQuery.create().addQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE), session)).hasSize(2);
    assertThat(dao.findByQuery(ComponentQuery.create().addIds(1L).addQualifiers(Qualifiers.PROJECT), session)).hasSize(1);
    assertThat(dao.findByQuery(ComponentQuery.create().addIds(1L).addQualifiers(Qualifiers.MODULE), session)).hasSize(0);
  }
}
