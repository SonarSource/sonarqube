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
package org.sonar.db.qualityprofile;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultQProfileDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE).setDisableDefaultOrganization(true);

  private DbSession dbSession = dbTester.getSession();
  private DefaultQProfileDao underTest = dbTester.getDbClient().defaultQProfileDao();

  @Test
  public void insertOrUpdate_inserts_row_when_does_not_exist() {
    OrganizationDto org = dbTester.organizations().insert();
    QProfileDto profile = dbTester.qualityProfiles().insert(org);
    DefaultQProfileDto dto = DefaultQProfileDto.from(profile);

    underTest.insertOrUpdate(dbSession, dto);
    dbSession.commit();

    assertThat(countRows()).isEqualTo(1);
    assertThatIsDefault(org, profile);
  }

  @Test
  public void insertOrUpdate_updates_row_when_exists() {
    OrganizationDto org = dbTester.organizations().insert();
    String previousQProfileUuid = Uuids.create();
    DefaultQProfileDto dto = new DefaultQProfileDto()
      .setLanguage("java")
      .setOrganizationUuid(org.getUuid())
      .setQProfileUuid(previousQProfileUuid);
    underTest.insertOrUpdate(dbSession, dto);
    dbSession.commit();

    String newQProfileUuid = Uuids.create();
    dto.setQProfileUuid(newQProfileUuid);
    underTest.insertOrUpdate(dbSession, dto);
    dbSession.commit();

    assertThat(countRows()).isEqualTo(1);
    assertThat(selectUuidOfDefaultProfile(org, dto.getLanguage())).hasValue(newQProfileUuid);
  }

  @Test
  public void deleteByQProfileUuids_deletes_rows_related_to_specified_profile() {
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    underTest.insertOrUpdate(dbSession, new DefaultQProfileDto().setOrganizationUuid(org1.getUuid()).setLanguage("java").setQProfileUuid("u1"));
    underTest.insertOrUpdate(dbSession, new DefaultQProfileDto().setOrganizationUuid(org1.getUuid()).setLanguage("js").setQProfileUuid("u2"));
    underTest.insertOrUpdate(dbSession, new DefaultQProfileDto().setOrganizationUuid(org2.getUuid()).setLanguage("java").setQProfileUuid("u3"));
    underTest.insertOrUpdate(dbSession, new DefaultQProfileDto().setOrganizationUuid(org2.getUuid()).setLanguage("js").setQProfileUuid("u4"));

    underTest.deleteByQProfileUuids(dbSession, asList("u1", "u3"));
    dbSession.commit();

    assertThat(countRows()).isEqualTo(2);
    assertThat(selectUuidOfDefaultProfile(org1, "java")).isEmpty();
    assertThat(selectUuidOfDefaultProfile(org1, "js")).hasValue("u2");
    assertThat(selectUuidOfDefaultProfile(org2, "java")).isEmpty();
    assertThat(selectUuidOfDefaultProfile(org2, "js")).hasValue("u4");
  }

  @Test
  public void selectExistingQProfileUuids_filters_defaults() {
    OrganizationDto org = dbTester.organizations().insert();
    QProfileDto profile1 = dbTester.qualityProfiles().insert(org);
    QProfileDto profile2 = dbTester.qualityProfiles().insert(org);
    dbTester.qualityProfiles().setAsDefault(profile1);

    List<String> profileUuids = asList(profile1.getKee(), profile2.getKee(), "other");
    assertThat(underTest.selectExistingQProfileUuids(dbSession, org.getUuid(), profileUuids))
      .containsExactly(profile1.getKee());
  }

  @Test
  public void isDefault_returns_true_if_profile_is_marked_as_default() {
    OrganizationDto org = dbTester.organizations().insert();
    QProfileDto profile1 = dbTester.qualityProfiles().insert(org);
    QProfileDto profile2 = dbTester.qualityProfiles().insert(org);
    dbTester.qualityProfiles().setAsDefault(profile1);

    assertThat(underTest.isDefault(dbSession, org.getUuid(), profile1.getKee())).isTrue();
    assertThat(underTest.isDefault(dbSession, org.getUuid(), profile2.getKee())).isFalse();
    assertThat(underTest.isDefault(dbSession, org.getUuid(), "does_not_exist")).isFalse();
  }

  @Test
  public void selectUuidsOfOrganizationsWithoutDefaultProfile() {
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    QProfileDto profileInOrg1 = dbTester.qualityProfiles().insert(org1, p -> p.setLanguage("java"));
    QProfileDto profileInOrg2 = dbTester.qualityProfiles().insert(org2, p -> p.setLanguage("java"));
    dbTester.qualityProfiles().setAsDefault(profileInOrg1);

    assertThat(underTest.selectUuidsOfOrganizationsWithoutDefaultProfile(dbSession, "java"))
      .containsExactly(org2.getUuid());
    assertThat(underTest.selectUuidsOfOrganizationsWithoutDefaultProfile(dbSession, "js"))
      .containsExactlyInAnyOrder(org1.getUuid(), org2.getUuid());
  }

  private void assertThatIsDefault(OrganizationDto org, QProfileDto profile) {
    assertThat(selectUuidOfDefaultProfile(org, profile.getLanguage())).hasValue(profile.getKee());
    assertThat(underTest.isDefault(dbSession, org.getUuid(), profile.getKee())).isTrue();
  }

  private int countRows() {
    return dbTester.countRowsOfTable("default_qprofiles");
  }

  private Optional<String> selectUuidOfDefaultProfile(OrganizationDto org, String language) {
    return dbTester.select("select qprofile_uuid as \"profileUuid\" " +
      " from default_qprofiles " +
      " where organization_uuid='" + org.getUuid() + "' and language='" + language + "'")
      .stream()
      .findFirst()
      .map(m -> (String) m.get("profileUuid"));
  }
}
