/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.usergroups;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.organization.TestDefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultGroupCreatorImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create();

  private DefaultGroupCreator underTest = new DefaultGroupCreatorImpl(db.getDbClient(), new SequenceUuidFactory(), TestDefaultOrganizationProvider.from(db));

  @Test
  public void create_default_group() {
    underTest.create(db.getSession());

    Optional<String> defaultGroupUuid = db.getDbClient().organizationDao().getDefaultGroupUuid(db.getSession(), db.getDefaultOrganization().getUuid());
    assertThat(defaultGroupUuid).isPresent();
    assertThat(db.getDbClient().groupDao().selectByUuid(db.getSession(), defaultGroupUuid.get()))
      .extracting(GroupDto::getName, GroupDto::getDescription)
      .containsOnly("Members", "All members of the organization");
  }

  @Test
  public void fail_with_IAE_when_default_group_already_exist() {
    OrganizationDto organizationDto = db.organizations().insert();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.organizations().setDefaultTemplates(permissionTemplate.getUuid(), null, null);
    db.users().insertGroup("Members");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("The group '%s' already exists", "Members"));

    underTest.create(db.getSession());
  }

}
