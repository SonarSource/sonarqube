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
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentUuidFactoryTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Test
  public void load_uuids_from_existing_components_in_db() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(project));

    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), project.getDbKey());
    assertThat(underTest.getOrCreateForKey(project.getDbKey())).isEqualTo(project.uuid());
    assertThat(underTest.getOrCreateForKey(module.getDbKey())).isEqualTo(module.uuid());
  }

  @Test
  public void generate_uuid_if_it_does_not_exist_in_db() {
    ComponentUuidFactory underTest = new ComponentUuidFactory(db.getDbClient(), db.getSession(), "theProjectKey");

    String generatedKey = underTest.getOrCreateForKey("foo");
    assertThat(generatedKey).isNotEmpty();

    // uuid is kept in memory for further calls with same key
    assertThat(underTest.getOrCreateForKey("foo")).isEqualTo(generatedKey);
  }

}
