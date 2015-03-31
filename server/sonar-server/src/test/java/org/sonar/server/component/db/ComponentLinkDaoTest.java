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

package org.sonar.server.component.db;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.component.ComponentLinkDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.test.DbTests;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ComponentLinkDaoTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  ComponentLinkDao dao;

  @Before
  public void createDao() throws Exception {
    session = dbTester.myBatis().openSession(false);
    dao = new ComponentLinkDao();
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void select_by_component_uuid() throws Exception {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentLinkDto> links = dao.selectByComponentUuid(session, "ABCD");
    assertThat(links).hasSize(2);

    links = dao.selectByComponentUuid(session, "BCDE");
    assertThat(links).hasSize(1);

    ComponentLinkDto link = links.get(0);
    assertThat(link.getId()).isEqualTo(3L);
    assertThat(link.getComponentUuid()).isEqualTo("BCDE");
    assertThat(link.getType()).isEqualTo("homepage");
    assertThat(link.getName()).isEqualTo("Home");
    assertThat(link.getHref()).isEqualTo("http://www.struts.org");
  }

  @Test
  public void insert() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    dao.insert(session, new ComponentLinkDto()
        .setComponentUuid("ABCD")
        .setType("homepage")
        .setName("Home")
        .setHref("http://www.sonarqube.org")
      );
    session.commit();

    dbTester.assertDbUnit(getClass(), "insert-result.xml", new String[]{"id"}, "project_links");
  }

  @Test
  public void update() throws Exception {
    dbTester.prepareDbUnit(getClass(), "update.xml");

    dao.update(session, new ComponentLinkDto()
      .setId(1L)
      .setComponentUuid("ABCD")
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org")
      );
    session.commit();

    dbTester.assertDbUnit(getClass(), "update-result.xml", "project_links");
  }

  @Test
  public void delete() throws Exception {
    dbTester.prepareDbUnit(getClass(), "delete.xml");

    dao.delete(session, 1L);
    session.commit();

    assertThat(dbTester.countRowsOfTable("project_links")).isEqualTo(0);
  }

}
