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
package org.sonar.server.platform.db.migration.version.v73;

import java.sql.SQLException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateSubscriptionOnOrganizationsTest {

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateSubscriptionOnOrganizationsTest.class, "schema.sql");

  private MapSettings settings = new MapSettings();

  private System2 system2 = new TestSystem2().setNow(NOW);

  private PopulateSubscriptionOnOrganizations underTest = new PopulateSubscriptionOnOrganizations(db.database(), system2, settings.asConfig());

  @Test
  public void set_organization_as_paid_when_containing_only_private_projects_on_sonarcloud() throws SQLException {
    setSonarCloud();
    String organization1 = insertOrganization(Uuids.createFast(), null);
    insertProject(organization1, true);
    insertProject(organization1, true);
    String organization2 = insertOrganization(Uuids.createFast(), null);
    insertProject(organization2, true);

    underTest.execute();

    assertOrganizations(
      tuple(organization1, "PAID", NOW),
      tuple(organization2, "PAID", NOW));
  }

  @Test
  public void set_organization_as_free_when_containing_no_private_projects_on_sonarcloud() throws SQLException {
    setSonarCloud();
    String organization1 = insertOrganization(Uuids.createFast(), null);
    insertProject(organization1, false);
    insertProject(organization1, false);
    String organization2 = insertOrganization(Uuids.createFast(), null);

    underTest.execute();

    assertOrganizations(
      tuple(organization1, "FREE", NOW),
      tuple(organization2, "FREE", NOW));
  }

  @Test
  public void does_nothing_when_subscription_is_already_set_on_sonarcloud() throws SQLException {
    setSonarCloud();
    String organization1 = insertOrganization(Uuids.createFast(), "PAID");
    insertProject(organization1, true);
    insertProject(organization1, true);
    String organization2 = insertOrganization(Uuids.createFast(), "FREE");
    insertProject(organization2, false);

    underTest.execute();

    assertOrganizations(
      tuple(organization1, "PAID", PAST),
      tuple(organization2, "FREE", PAST));
  }

  @Test
  public void migration_is_reentrant_on_sonarcloud() throws SQLException {
    setSonarCloud();
    String organization1 = insertOrganization(Uuids.createFast(), null);
    insertProject(organization1, true);
    insertProject(organization1, true);
    String organization2 = insertOrganization(Uuids.createFast(), null);
    insertProject(organization2, false);

    underTest.execute();
    underTest.execute();

    assertOrganizations(
      tuple(organization1, "PAID", NOW),
      tuple(organization2, "FREE", NOW));
  }

  @Test
  public void set_organization_as_sonarqube() throws SQLException {
    String defaultOrganization = insertOrganization(Uuids.createFast(), null);

    underTest.execute();

    assertOrganizations(tuple(defaultOrganization, "SONARQUBE", NOW));
  }

  @Test
  public void does_nothing_when_subscription_is_already_set_on_sonarqube() throws SQLException {
    String defaultOrganization = insertOrganization(Uuids.createFast(), "SONARQUBE");

    underTest.execute();

    assertOrganizations(tuple(defaultOrganization, "SONARQUBE", PAST));
  }

  @Test
  public void migration_is_reentrant_on_sonarqube() throws SQLException {
    String defaultOrganization = insertOrganization(Uuids.createFast(), null);

    underTest.execute();
    underTest.execute();

    assertOrganizations(tuple(defaultOrganization, "SONARQUBE", NOW));
  }

  private void assertOrganizations(Tuple... expectedTuples) {
    assertThat(db.select("SELECT UUID, SUBSCRIPTION, UPDATED_AT FROM ORGANIZATIONS")
      .stream()
      .map(row -> new Tuple(row.get("UUID"), row.get("SUBSCRIPTION"), row.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private String insertOrganization(String uuid, @Nullable String subscription) {
    db.executeInsert("ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "SUBSCRIPTION", subscription,
      "GUARDED", false,
      "DEFAULT_QUALITY_GATE_UUID", "QGATE_UUID",
      "NEW_PROJECT_PRIVATE", false,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    return uuid;
  }

  private void insertProject(String organizationUuid, boolean isPrivate) {
    String uuid = Uuids.createFast();
    db.executeInsert("PROJECTS",
      "ORGANIZATION_UUID", organizationUuid,
      "KEE", uuid + "-key",
      "UUID", uuid,
      "PROJECT_UUID", uuid,
      "MAIN_BRANCH_PROJECT_UUID", uuid,
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", Boolean.toString(isPrivate),
      "SCOPE", "PRJ",
      "QUALIFIER", "PRJ");
  }

  private void setSonarCloud() {
    settings.setProperty("sonar.sonarcloud.enabled", true);
  }
}
