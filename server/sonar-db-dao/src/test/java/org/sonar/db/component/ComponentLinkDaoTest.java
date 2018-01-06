/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentLinkDto.TYPE_HOME_PAGE;
import static org.sonar.db.component.ComponentLinkDto.TYPE_SOURCES;
import static org.sonar.db.component.ComponentLinkTesting.newComponentLinkDto;


public class ComponentLinkDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private ComponentLinkDao underTest = db.getDbClient().componentLinkDao();

  @Test
  public void select_by_component_uuid() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ComponentLinkDto> links = underTest.selectByComponentUuid(db.getSession(), "ABCD");
    assertThat(links).hasSize(2);

    links = underTest.selectByComponentUuid(db.getSession(), "BCDE");
    assertThat(links).hasSize(1);

    ComponentLinkDto link = links.get(0);
    assertThat(link.getId()).isEqualTo(3L);
    assertThat(link.getComponentUuid()).isEqualTo("BCDE");
    assertThat(link.getType()).isEqualTo("homepage");
    assertThat(link.getName()).isEqualTo("Home");
    assertThat(link.getHref()).isEqualTo("http://www.struts.org");
  }

  @Test
  public void select_by_component_uuids() {
    String firstUuid = "COMPONENT_UUID_1";
    String secondUuid = "COMPONENT_UUID_2";
    dbClient.componentLinkDao().insert(dbSession, newComponentLinkDto().setComponentUuid(firstUuid).setType(TYPE_HOME_PAGE));
    dbClient.componentLinkDao().insert(dbSession, newComponentLinkDto().setComponentUuid(firstUuid).setType(TYPE_SOURCES));
    dbClient.componentLinkDao().insert(dbSession, newComponentLinkDto().setComponentUuid(secondUuid).setType(TYPE_HOME_PAGE));
    dbClient.componentLinkDao().insert(dbSession, newComponentLinkDto().setComponentUuid("ANOTHER_COMPONENT_UUID").setType(TYPE_HOME_PAGE));
    db.commit();

    List<ComponentLinkDto> result = underTest.selectByComponentUuids(dbSession, newArrayList(firstUuid, secondUuid));

    assertThat(result).hasSize(3).extracting(ComponentLinkDto::getComponentUuid).containsOnly(firstUuid, secondUuid);
  }

  @Test
  public void select_by_component_uuids_with_empty_list() {
    List<ComponentLinkDto> result = underTest.selectByComponentUuids(dbSession, emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  public void select_by_id() {
    ComponentLinkDto link = underTest.insert(dbSession, newComponentLinkDto());
    db.commit();

    ComponentLinkDto candidate = underTest.selectById(dbSession, link.getId());
    assertThat(candidate.getId()).isNotNull();
  }

  @Test
  public void insert() {
    db.prepareDbUnit(getClass(), "empty.xml");

    underTest.insert(db.getSession(), new ComponentLinkDto()
      .setComponentUuid("ABCD")
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org")
      );
    db.getSession().commit();

    db.assertDbUnit(getClass(), "insert-result.xml", new String[] {"id"}, "project_links");
  }

  @Test
  public void update() {
    db.prepareDbUnit(getClass(), "update.xml");

    underTest.update(db.getSession(), new ComponentLinkDto()
      .setId(1L)
      .setComponentUuid("ABCD")
      .setType("homepage")
      .setName("Home")
      .setHref("http://www.sonarqube.org")
      );
    db.getSession().commit();

    db.assertDbUnit(getClass(), "update-result.xml", "project_links");
  }

  @Test
  public void delete() {
    db.prepareDbUnit(getClass(), "delete.xml");

    underTest.delete(db.getSession(), 1L);
    db.getSession().commit();

    assertThat(db.countRowsOfTable("project_links")).isEqualTo(0);
  }

}
