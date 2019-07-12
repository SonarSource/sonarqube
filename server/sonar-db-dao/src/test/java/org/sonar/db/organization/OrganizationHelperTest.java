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
package org.sonar.db.organization;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

@RunWith(DataProviderRunner.class)
public class OrganizationHelperTest {

  private static final Random RANDOM = new Random();

  @Rule
  public DbTester db = DbTester.create(mock(System2.class)).setDisableDefaultOrganization(true);
  public DbSession dbSession = db.getSession();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final OrganizationHelper underTest = new OrganizationHelper(db.getDbClient());

  @Test
  public void returns_empty_list_when_user_is_not_admin_of_any_orgs() {
    UserDto user1 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org1);
    db.users().insertMember(group1, user1);

    assertThat(underTest.selectOrganizationsWithLastAdmin(dbSession, user1.getId())).isEmpty();
  }

  @Test
  public void returns_orgs_where_user_is_last_admin() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert();
    OrganizationDto org2 = db.organizations().insert();

    setAsDirectOrIndirectAdmin(user1, org1);
    setAsDirectOrIndirectAdmin(user2, org1);
    setAsDirectOrIndirectAdmin(user1, org2);

    assertThat(underTest.selectOrganizationsWithLastAdmin(dbSession, user1.getId()))
      .extracting(OrganizationDto::getKey)
      .containsExactly(org2.getKey());
  }

  @Test
  @UseDataProvider("adminUserCombinationsAndExpectedOrgKeys")
  public void returns_correct_orgs_for_interesting_combinations_of_last_admin_or_not(
    boolean user2IsAdminOfOrg1, boolean user1IsAdminOfOrg2, boolean user2IsAdminOfOrg2, List<String> expectedOrgKeys) {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    OrganizationDto org1 = db.organizations().insert(o -> o.setKey("org1"));
    OrganizationDto org2 = db.organizations().insert(o -> o.setKey("org2"));

    setAsDirectOrIndirectAdmin(user1, org1);
    if (user2IsAdminOfOrg1) {
      setAsDirectOrIndirectAdmin(user2, org1);
    }
    if (user1IsAdminOfOrg2) {
      setAsDirectOrIndirectAdmin(user1, org2);
    }
    if (user2IsAdminOfOrg2) {
      setAsDirectOrIndirectAdmin(user2, org2);
    }

    assertThat(underTest.selectOrganizationsWithLastAdmin(dbSession, user1.getId()))
      .extracting(OrganizationDto::getKey)
      .containsExactlyInAnyOrderElementsOf(expectedOrgKeys);
  }

  @DataProvider
  public static Object[][] adminUserCombinationsAndExpectedOrgKeys() {
    return new Object[][] {
      // note: user1 is always admin of org1
      // param 1: user2 is admin of org1
      // param 2: user1 is admin of org2
      // param 3: user2 is admin of org2
      // param 4: list of orgs preventing user1 to delete
      {true, true, true, emptyList()},
      {true, true, false, singletonList("org2")},
      {true, false, true, emptyList()},
      {true, false, false, emptyList()},
      {false, true, true, singletonList("org1")},
      {false, true, false, asList("org1", "org2")},
      {false, false, true, singletonList("org1")},
      {false, false, false, singletonList("org1")},
    };
  }

  private void setAsDirectOrIndirectAdmin(UserDto user, OrganizationDto organization) {
    boolean useDirectAdmin = RANDOM.nextBoolean();
    if (useDirectAdmin) {
      db.users().insertPermissionOnUser(organization, user, ADMINISTER);
    } else {
      GroupDto group = db.users().insertGroup(organization);
      db.users().insertPermissionOnGroup(group, ADMINISTER);
      db.users().insertMember(group, user);
    }
  }
}
