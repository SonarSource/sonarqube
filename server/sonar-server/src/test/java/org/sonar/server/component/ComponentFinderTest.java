/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.exceptions.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;


public class ComponentFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private ComponentFinder underTest = TestComponentFinder.from(db);

  @Test
  public void fail_when_the_uuid_and_key_are_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'key' must be provided, not both");

    underTest.getByUuidOrKey(dbSession, null, null, ID_AND_KEY);
  }

  @Test
  public void fail_when_the_uuid_and_key_are_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'id' or 'key' must be provided, not both");

    underTest.getByUuidOrKey(dbSession, "project-uuid", "project-key", ID_AND_KEY);
  }

  @Test
  public void fail_when_the_uuid_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'id' parameter must not be empty");

    underTest.getByUuidOrKey(dbSession, "", null, ID_AND_KEY);
  }

  @Test
  public void fail_when_the_key_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter must not be empty");

    underTest.getByUuidOrKey(dbSession, null, "", ID_AND_KEY);
  }

  @Test
  public void fail_when_component_uuid_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'project-uuid' not found");

    underTest.getByUuidOrKey(dbSession, "project-uuid", null, ID_AND_KEY);
  }

  @Test
  public void fail_when_component_key_not_found() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'project-key' not found");

    underTest.getByUuidOrKey(dbSession, null, "project-key", ID_AND_KEY);
  }


  @Test
  public void get_component_by_uuid() {
    db.components().insertComponent(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));

    ComponentDto component = underTest.getByUuidOrKey(dbSession, "project-uuid", null, ID_AND_KEY);

    assertThat(component.uuid()).isEqualTo("project-uuid");
  }

  @Test
  public void get_component_by_key() {
    db.components().insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setKey("project-key"));

    ComponentDto component = underTest.getByUuidOrKey(dbSession, null, "project-key", ID_AND_KEY);

    assertThat(component.key()).isEqualTo("project-key");
  }
}
