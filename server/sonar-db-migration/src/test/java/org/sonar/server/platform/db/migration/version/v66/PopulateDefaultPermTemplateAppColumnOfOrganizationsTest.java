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

package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import java.util.Random;
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
import static org.assertj.core.groups.Tuple.tuple;

public class PopulateDefaultPermTemplateAppColumnOfOrganizationsTest {

  private static final long PAST = 10_000_000_000L;
  private static final long NOW = 20_000_000_000L;
  private static final Random RANDOM = new Random();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateDefaultPermTemplateAppColumnOfOrganizationsTest.class, "initial.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = new TestSystem2().setNow(NOW);

  private PopulateDefaultPermTemplateAppColumnOfOrganizations underTest = new PopulateDefaultPermTemplateAppColumnOfOrganizations(db.database(), system2);

  @Test
  public void copy_template_from_view() throws SQLException {
    String projectTemplateKey = "PROJECT_PERM-" + RANDOM.nextInt();
    String viewTemplateKey = "VIEW_PERM-" + RANDOM.nextInt();
    String organization = insertOrganization(projectTemplateKey, viewTemplateKey, null);

    underTest.execute();

    assertOrganizations(tuple(organization, viewTemplateKey, NOW));
  }

  @Test
  public void does_nothing_when_no_view_template() throws SQLException {
    String projectTemplateKey = "PROJECT_PERM-" + RANDOM.nextInt();
    String organization = insertOrganization(projectTemplateKey, null, null);

    underTest.execute();

    assertOrganizations(tuple(organization, null, PAST));
  }

  @Test
  public void does_nothing_when_application_template_already_exist() throws SQLException {
    String projectTemplateKey = "PROJECT_PERM-" + RANDOM.nextInt();
    String viewTemplateKey = "VIEW_PERM-" + RANDOM.nextInt();
    String applicationTemplateKey = "PERM-" + RANDOM.nextInt();
    String organization = insertOrganization(projectTemplateKey, viewTemplateKey, applicationTemplateKey);

    underTest.execute();

    assertOrganizations(tuple(organization, applicationTemplateKey, PAST));
  }

  @Test
  public void update_many_organizations() throws SQLException {
    String projectTemplateKey = "PROJECT_PERM-" + RANDOM.nextInt();
    String viewTemplateKey1 = "VIEW_PERM-" + RANDOM.nextInt();
    String viewTemplateKey2 = "VIEW_PERM-" + RANDOM.nextInt();
    String viewTemplateKey3 = "VIEW_PERM-" + RANDOM.nextInt();
    String organization1 = insertOrganization(projectTemplateKey, viewTemplateKey1, null);
    String organization2 = insertOrganization(projectTemplateKey, viewTemplateKey2, null);
    String organization3 = insertOrganization(projectTemplateKey, viewTemplateKey3, null);

    underTest.execute();

    assertOrganizations(
      tuple(organization1, viewTemplateKey1, NOW),
      tuple(organization2, viewTemplateKey2, NOW),
      tuple(organization3, viewTemplateKey3, NOW)
    );
  }

  private void assertOrganizations(Tuple... expectedTuples) {
    assertThat(db.select("SELECT UUID, DEFAULT_PERM_TEMPLATE_APP, UPDATED_AT FROM ORGANIZATIONS")
      .stream()
      .map(map -> new Tuple(map.get("UUID"), map.get("DEFAULT_PERM_TEMPLATE_APP"), map.get("UPDATED_AT")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private String insertOrganization(String projectDefaultTemplate, @Nullable String viewDefaultTemplate, @Nullable String applicationDefaultTemplate) {
    String uuid = "ORG-" + RANDOM.nextInt();
    db.executeInsert(
      "ORGANIZATIONS",
      "UUID", uuid,
      "KEE", uuid,
      "NAME", uuid,
      "DEFAULT_PERM_TEMPLATE_PROJECT", projectDefaultTemplate,
      "DEFAULT_PERM_TEMPLATE_VIEW", viewDefaultTemplate,
      "DEFAULT_PERM_TEMPLATE_APP", applicationDefaultTemplate,
      "GUARDED", "false",
      "NEW_PROJECT_PRIVATE", "false",
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    return uuid;
  }

}
