/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultQProfileDaoIT {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();

  private DefaultQProfileDao underTest = dbTester.getDbClient().defaultQProfileDao();

  @Test
  public void insertOrUpdate_inserts_row_when_does_not_exist() {
    QProfileDto profile = dbTester.qualityProfiles().insert();
    DefaultQProfileDto dto = DefaultQProfileDto.from(profile);

    underTest.insertOrUpdate(dbSession, dto);
    dbSession.commit();

    assertThat(countRows()).isOne();
    assertThatIsDefault(profile);
  }

  @Test
  public void insertOrUpdate_updates_row_when_exists() {
    String previousQProfileUuid = Uuids.create();
    DefaultQProfileDto dto = new DefaultQProfileDto()
      .setLanguage("java")
      .setQProfileUuid(previousQProfileUuid);
    underTest.insertOrUpdate(dbSession, dto);
    dbSession.commit();

    String newQProfileUuid = Uuids.create();
    dto.setQProfileUuid(newQProfileUuid);
    underTest.insertOrUpdate(dbSession, dto);
    dbSession.commit();

    assertThat(countRows()).isOne();
    assertThat(selectUuidOfDefaultProfile(dto.getLanguage())).hasValue(newQProfileUuid);
  }

  @Test
  public void insert_row() {
    String previousQProfileUuid = Uuids.create();
    DefaultQProfileDto dto = new DefaultQProfileDto()
      .setLanguage("java")
      .setQProfileUuid(previousQProfileUuid);
    underTest.insert(dbSession, dto);
    dbSession.commit();
      assertThat(countRows()).isOne();
      assertThat(selectUuidOfDefaultProfile(dto.getLanguage())).hasValue(dto.getQProfileUuid());
  }

  @Test
  public void deleteByQProfileUuids_deletes_rows_related_to_specified_profile() {
    underTest.insertOrUpdate(dbSession, new DefaultQProfileDto().setLanguage("java").setQProfileUuid("u1"));
    underTest.insertOrUpdate(dbSession, new DefaultQProfileDto().setLanguage("js").setQProfileUuid("u2"));

    underTest.deleteByQProfileUuids(dbSession, asList("u1", "u3"));
    dbSession.commit();

    assertThat(countRows()).isOne();
    assertThat(selectUuidOfDefaultProfile("java")).isEmpty();
    assertThat(selectUuidOfDefaultProfile("js")).hasValue("u2");
  }

  @Test
  public void selectExistingQProfileUuids_filters_defaults() {
    QProfileDto profile1 = dbTester.qualityProfiles().insert();
    QProfileDto profile2 = dbTester.qualityProfiles().insert();
    dbTester.qualityProfiles().setAsDefault(profile1);

    List<String> profileUuids = asList(profile1.getKee(), profile2.getKee(), "other");
    assertThat(underTest.selectExistingQProfileUuids(dbSession, profileUuids))
      .containsExactly(profile1.getKee());
  }

  @Test
  public void isDefault_returns_true_if_profile_is_marked_as_default() {
    QProfileDto profile1 = dbTester.qualityProfiles().insert();
    QProfileDto profile2 = dbTester.qualityProfiles().insert();
    dbTester.qualityProfiles().setAsDefault(profile1);

    assertThat(underTest.isDefault(dbSession, profile1.getKee())).isTrue();
    assertThat(underTest.isDefault(dbSession, profile2.getKee())).isFalse();
    assertThat(underTest.isDefault(dbSession, "does_not_exist")).isFalse();
  }

  private void assertThatIsDefault(QProfileDto profile) {
    assertThat(selectUuidOfDefaultProfile(profile.getLanguage())).hasValue(profile.getKee());
    assertThat(underTest.isDefault(dbSession, profile.getKee())).isTrue();
  }

  private int countRows() {
    return dbTester.countRowsOfTable("default_qprofiles");
  }

  private Optional<String> selectUuidOfDefaultProfile(String language) {
    return dbTester.select("select qprofile_uuid as \"profileUuid\" from default_qprofiles where language='" + language + "'")
      .stream()
      .findFirst()
      .map(m -> (String) m.get("profileUuid"));
  }
}
