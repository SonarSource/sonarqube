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
package org.sonar.db.organization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.exceptions.PersistenceException;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

public class OrganizationMemberDaoTest {
  @Rule
  public final DbTester db = DbTester.create().setDisableDefaultOrganization(true);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private OrganizationMemberDao underTest = dbClient.organizationMemberDao();

  @Test
  public void select() {
    underTest.insert(dbSession, create("O1", 512));

    Optional<OrganizationMemberDto> result = underTest.select(dbSession, "O1", 512);

    assertThat(result).isPresent();
    assertThat(result.get()).extracting(OrganizationMemberDto::getOrganizationUuid, OrganizationMemberDto::getUserId).containsExactly("O1", 512);
    assertThat(underTest.select(dbSession, "O1", 256)).isNotPresent();
    assertThat(underTest.select(dbSession, "O2", 512)).isNotPresent();
  }

  @Test
  public void select_logins() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.organizations().addMember(organization, anotherUser);
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);

    List<String> result = underTest.selectLoginsByOrganizationUuid(dbSession, organization.getUuid());

    assertThat(result).containsOnly(user.getLogin(), anotherUser.getLogin());
  }

  @Test
  public void select_user_ids() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    UserDto userInAnotherOrganization = db.users().insertUser();
    db.organizations().addMember(organization, user);
    db.organizations().addMember(organization, anotherUser);
    db.organizations().addMember(anotherOrganization, userInAnotherOrganization);

    List<Integer> result = underTest.selectUserIdsByOrganizationUuid(dbSession, organization.getUuid());

    assertThat(result).containsOnly(user.getId(), anotherUser.getId());
  }

  @Test
  public void select_organization_uuids_by_user_id() {
    OrganizationDto organizationDto1 = db.organizations().insert();
    OrganizationDto organizationDto2 = db.organizations().insert();
    OrganizationDto organizationDto3 = db.organizations().insert();
    underTest.insert(dbSession, create(organizationDto1.getUuid(), 512));
    underTest.insert(dbSession, create(organizationDto2.getUuid(), 512));

    assertThat(underTest.selectOrganizationUuidsByUser(dbSession, 512)).containsOnly(organizationDto1.getUuid(), organizationDto2.getUuid())
      .doesNotContain(organizationDto3.getUuid());
    assertThat(underTest.selectOrganizationUuidsByUser(dbSession, 123)).isEmpty();
  }

  @Test
  public void select_for_indexing() {
    OrganizationDto org1 = db.organizations().insert(o -> o.setUuid("ORG_1"));
    OrganizationDto org2 = db.organizations().insert(o -> o.setUuid("ORG_2"));
    UserDto user1 = db.users().insertUser("L_1");
    UserDto user2 = db.users().insertUser("L_2");
    db.organizations().addMember(org1, user1);
    db.organizations().addMember(org1, user2);
    db.organizations().addMember(org2, user1);
    List<Tuple> result = new ArrayList<>();

    underTest.selectForUserIndexing(dbSession, Arrays.asList("L_1", "L_2"), (login, org) -> result.add(tuple(login, org)));

    assertThat(result).containsOnly(tuple("L_1", "ORG_1"), tuple("L_1", "ORG_2"), tuple("L_2", "ORG_1"));
  }

  @Test
  public void select_all_for_indexing() {
    OrganizationDto org1 = db.organizations().insert(o -> o.setUuid("ORG_1"));
    OrganizationDto org2 = db.organizations().insert(o -> o.setUuid("ORG_2"));
    UserDto user1 = db.users().insertUser("L_1");
    UserDto user2 = db.users().insertUser("L_2");
    db.organizations().addMember(org1, user1);
    db.organizations().addMember(org1, user2);
    db.organizations().addMember(org2, user1);
    List<Tuple> result = new ArrayList<>();

    underTest.selectAllForUserIndexing(dbSession, (login, org) -> result.add(tuple(login, org)));

    assertThat(result).containsOnly(tuple("L_1", "ORG_1"), tuple("L_1", "ORG_2"), tuple("L_2", "ORG_1"));
  }

  @Test
  public void insert() {
    underTest.insert(dbSession, create("O_1", 256));

    Map<String, Object> result = db.selectFirst(dbSession, "select organization_uuid as \"organizationUuid\", user_id as \"userId\" from organization_members");

    assertThat(result).containsOnly(entry("organizationUuid", "O_1"), entry("userId", 256L));
  }

  @Test
  public void fail_insert_if_no_organization_uuid() {
    expectedException.expect(PersistenceException.class);

    underTest.insert(dbSession, create(null, 256));
  }

  @Test
  public void fail_insert_if_no_user_id() {
    expectedException.expect(PersistenceException.class);

    underTest.insert(dbSession, create("O_1", null));
  }

  @Test
  public void fail_if_organization_member_already_exist() {
    underTest.insert(dbSession, create("O_1", 256));
    expectedException.expect(PersistenceException.class);

    underTest.insert(dbSession, create("O_1", 256));
  }

  @Test
  public void delete_by_organization() {
    underTest.insert(dbSession, create("O1", 512));
    underTest.insert(dbSession, create("O1", 513));
    underTest.insert(dbSession, create("O2", 512));

    underTest.deleteByOrganizationUuid(dbSession, "O1");

    assertThat(underTest.select(dbSession, "O1", 512)).isNotPresent();
    assertThat(underTest.select(dbSession, "O1", 513)).isNotPresent();
    assertThat(underTest.select(dbSession, "O2", 512)).isPresent();
  }

  @Test
  public void delete_by_user_id() {
    underTest.insert(dbSession, create("O1", 512));
    underTest.insert(dbSession, create("O1", 513));
    underTest.insert(dbSession, create("O2", 512));

    underTest.deleteByUserId(dbSession, 512);
    db.commit();

    assertThat(db.select("select organization_uuid as \"organizationUuid\", user_id as \"userId\" from organization_members"))
      .extracting((row) -> row.get("organizationUuid"), (row) -> row.get("userId"))
      .containsOnly(tuple("O1", 513L));
  }

  private OrganizationMemberDto create(String organizationUuid, Integer userId) {
    return new OrganizationMemberDto()
      .setOrganizationUuid(organizationUuid)
      .setUserId(userId);
  }
}
