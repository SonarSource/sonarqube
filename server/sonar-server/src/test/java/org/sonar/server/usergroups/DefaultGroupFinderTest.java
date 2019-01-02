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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultGroupFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create();

  private DefaultGroupFinder underTest = new DefaultGroupFinder(db.getDbClient());

  @Test
  public void find_default_group() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "default");

    GroupDto result = underTest.findDefaultGroup(db.getSession(), organization.getUuid());

    assertThat(result.getId()).isEqualTo(defaultGroup.getId());
    assertThat(result.getName()).isEqualTo("default");
  }

  @Test
  public void fail_with_ISE_when_no_default_group_on_org() {
    OrganizationDto organization = db.organizations().insert();
    db.users().insertGroup(organization);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(format("Default group cannot be found on organization '%s'", organization.getUuid()));

    underTest.findDefaultGroup(db.getSession(), organization.getUuid());
  }

  @Test
  public void fail_with_NPE_when_default_group_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(organization, "default");
    db.getDbClient().groupDao().deleteById(db.getSession(), defaultGroup.getId());

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage(format("Group '%s' cannot be found", defaultGroup.getId()));

    underTest.findDefaultGroup(db.getSession(), organization.getUuid());
  }
}
