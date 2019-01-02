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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateDefaultPermTemplateOnOrganizationsTest {

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateDefaultPermTemplateOnOrganizationsTest.class, "organizations.sql");
  private System2 system2 = new TestSystem2().setNow(NOW);

  private PopulateDefaultPermTemplateOnOrganizations underTest = new PopulateDefaultPermTemplateOnOrganizations(db.database(), system2);

  @Test
  public void test_is_reentrant() throws SQLException {
    insertOrganization("aa", "aa-1");
    insertOrganization("bb", null);
    underTest.execute();
    underTest.execute();

    assertOrganizations(
      tuple("aa", "aa-1", "aa-1", "aa-1", NOW),
      tuple("bb", null, null, null, PAST)
    );
  }

  @Test
  public void test_with_organizations() throws SQLException {
    insertOrganization("aa", "aa-1");
    insertOrganization("bb", "bb-1");
    insertOrganization("cc", null);

    underTest.execute();

    assertOrganizations(
      tuple("aa", "aa-1", "aa-1", "aa-1", NOW),
      tuple("bb", "bb-1", "bb-1", "bb-1", NOW),
      tuple("cc", null, null, null, PAST)
    );
  }

  @Test
  public void without_governance_no_modifications() throws SQLException {
    insertOrganization("default-organization", null);

    underTest.execute();

    assertOrganizations(
      tuple("default-organization", null, null, null, PAST)
    );
  }

  private void assertOrganizations(Tuple... expectedTuples) {
    assertThat(db.select("SELECT uuid, default_perm_template_view, default_perm_template_app, default_perm_template_port, updated_at FROM organizations")
      .stream()
      .map(row -> new Tuple(row.get("UUID"), row.get("DEFAULT_PERM_TEMPLATE_VIEW"), row.get("DEFAULT_PERM_TEMPLATE_APP"), row.get("DEFAULT_PERM_TEMPLATE_PORT"), row.get("UPDATED_AT")))
      .collect(Collectors.toList()))
      .containsExactlyInAnyOrder(expectedTuples);
  }

  private void insertOrganization(String uuid, @Nullable String defaultPermTemplateView) {
    db.executeInsert("ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "GUARDED", false,
      "DEFAULT_PERM_TEMPLATE_VIEW", defaultPermTemplateView,
      "DEFAULT_QUALITY_GATE_UUID", "111",
      "NEW_PROJECT_PRIVATE", false,
      "SUBSCRIPTION", "sonarqube",
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
  }
}
