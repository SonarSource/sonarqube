/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultGroupCreatorImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create();

  private DefaultGroupCreator underTest = new DefaultGroupCreatorImpl(db.getDbClient());

  @Test
  public void create_default_group() {
    OrganizationDto organizationDto = db.organizations().insert();

    underTest.create(db.getSession(), organizationDto.getUuid());

    Optional<Integer> defaultGroupId = db.getDbClient().organizationDao().getDefaultGroupId(db.getSession(), organizationDto.getUuid());
    assertThat(defaultGroupId).isPresent();
    assertThat(db.getDbClient().groupDao().selectById(db.getSession(), defaultGroupId.get()))
      .extracting(GroupDto::getName, GroupDto::getDescription)
      .containsOnly("Members", "All members of the organization");
  }

  @Test
  public void fail_with_IAE_when_default_group_already_exist() {
    OrganizationDto organizationDto = db.organizations().insert();
    PermissionTemplateDto permissionTemplate = db.permissionTemplates().insertTemplate();
    db.organizations().setDefaultTemplates(organizationDto, permissionTemplate.getUuid(), null, null);
    db.users().insertGroup(organizationDto, "Members");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("The group '%s' already exist on organization '%s'", "Members", organizationDto.getUuid()));

    underTest.create(db.getSession(), organizationDto.getUuid());
  }

}
