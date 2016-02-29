/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.component;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;


public class ComponentLinkDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ComponentLinkDao dao = dbTester.getDbClient().componentLinkDao();

  @Test
  public void select_by_component_uuid() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentLinkDto> links = dao.selectByComponentUuid(dbTester.getSession(), "ABCD");
    assertThat(links).hasSize(2);

    links = dao.selectByComponentUuid(dbTester.getSession(), "BCDE");
    assertThat(links).hasSize(1);

    ComponentLinkDto link = links.get(0);
    assertThat(link.getId()).isEqualTo(3L);
    assertThat(link.getComponentUuid()).isEqualTo("BCDE");
    assertThat(link.getType()).isEqualTo("homepage");
    assertThat(link.getName()).isEqualTo("Home");
    assertThat(link.getHref()).isEqualTo("http://www.struts.org");
  }

  @Test
  public void insert() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    dao.insert(dbTester.getSession(), new ComponentLinkDto()
      .setComponentUuid("ABCD")
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org")
      );
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "insert-result.xml", new String[] {"id"}, "project_links");
  }

  @Test
  public void update() {
    dbTester.prepareDbUnit(getClass(), "update.xml");

    dao.update(dbTester.getSession(), new ComponentLinkDto()
      .setId(1L)
      .setComponentUuid("ABCD")
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org")
      );
    dbTester.getSession().commit();

    dbTester.assertDbUnit(getClass(), "update-result.xml", "project_links");
  }

  @Test
  public void delete() {
    dbTester.prepareDbUnit(getClass(), "delete.xml");

    dao.delete(dbTester.getSession(), 1L);
    dbTester.getSession().commit();

    assertThat(dbTester.countRowsOfTable("project_links")).isEqualTo(0);
  }

}
