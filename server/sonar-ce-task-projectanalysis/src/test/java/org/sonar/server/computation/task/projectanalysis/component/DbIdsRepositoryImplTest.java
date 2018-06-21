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
package org.sonar.server.computation.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;

public class DbIdsRepositoryImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String SOME_COMPONENT_KEY = "SOME_COMPONENT_KEY";
  private static final Component SOME_COMPONENT = ReportComponent.builder(PROJECT, 1).setKey(SOME_COMPONENT_KEY).build();
  private static final Developer SOME_DEVELOPER = new DumbDeveloper("DEV1");

  @Test
  public void add_and_get_component_id() {
    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setComponentId(SOME_COMPONENT, 10L);

    assertThat(cache.getComponentId(SOME_COMPONENT)).isEqualTo(10L);
  }

  @Test
  public void fail_to_get_component_id_on_unknown_ref() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No component id registered in repository for Component '" + SOME_COMPONENT_KEY + "'");

    new DbIdsRepositoryImpl().getComponentId(SOME_COMPONENT);
  }

  @Test
  public void fail_if_component_id_already_set() {
    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setComponentId(SOME_COMPONENT, 10L);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Component id '10' is already registered in repository for Component '" + SOME_COMPONENT_KEY + "', can not set new id '11'");
    cache.setComponentId(SOME_COMPONENT, 11L);
  }

  @Test
  public void add_and_get_developer_id() {
    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setDeveloperId(SOME_DEVELOPER, 100L);

    assertThat(cache.getDeveloperId(SOME_DEVELOPER)).isEqualTo(100L);
  }

  @Test
  public void fail_to_get_developer_id_on_unknown_developer() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("No id registered in repository for Developer '" + SOME_DEVELOPER + "'");

    new DbIdsRepositoryImpl().getDeveloperId(SOME_DEVELOPER);
  }

  @Test
  public void fail_if_developer_id_already_set() {
    DbIdsRepositoryImpl cache = new DbIdsRepositoryImpl();
    cache.setDeveloperId(SOME_DEVELOPER, 10L);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Id '10' is already registered in repository for Developer '" + SOME_DEVELOPER + "', can not set new id '11'");
    cache.setDeveloperId(SOME_DEVELOPER, 11L);
  }

}
