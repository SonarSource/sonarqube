/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class MoveDefaultTemplatesToInternalPropertiesTest {

  private static final long NOW = 100_000_000_000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MoveDefaultTemplatesToInternalPropertiesTest.class, "schema.sql");

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final System2 system2 = new TestSystem2().setNow(NOW);

  private final DataChange underTest = new MoveDefaultTemplatesToInternalProperties(db.database(), system2);

  @Test
  public void create_internal_properties_for_all_components() throws SQLException {
    insertDefaultOrganization("PRJ", "PORT", "APP");

    underTest.execute();

    assertInternalProperties(
      tuple("defaultTemplate.prj", "PRJ", NOW),
      tuple("defaultTemplate.port", "PORT", NOW),
      tuple("defaultTemplate.app", "APP", NOW)
    );
  }

  @Test
  public void create_internal_properties_when_only_template_for_project() throws SQLException {
    insertDefaultOrganization("PRJ", null, null);

    underTest.execute();

    assertInternalProperties(
      tuple("defaultTemplate.prj", "PRJ", NOW));
  }

  @Test
  public void create_internal_properties_when_template_for_project_and_portfolio() throws SQLException {
    insertDefaultOrganization("PRJ", "PORT", null);

    underTest.execute();

    assertInternalProperties(
      tuple("defaultTemplate.prj", "PRJ", NOW),
      tuple("defaultTemplate.port", "PORT", NOW)
    );
  }

  @Test
  public void create_internal_properties_when_template_for_project_and_application() throws SQLException {
    insertDefaultOrganization("PRJ", null, "APP");

    underTest.execute();

    assertInternalProperties(
      tuple("defaultTemplate.prj", "PRJ", NOW),
      tuple("defaultTemplate.app", "APP", NOW)
    );
  }

  @Test
  public void do_nothing_when_permission_template_for_project_is_missing() throws SQLException {
    insertDefaultOrganization(null, null, "APP");

    underTest.execute();

    assertThat(db.countRowsOfTable("internal_properties")).isZero();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertDefaultOrganization("PRJ", "PORT", "APP");

    underTest.execute();
    underTest.execute();

    assertInternalProperties(
      tuple("defaultTemplate.prj", "PRJ", NOW),
      tuple("defaultTemplate.port", "PORT", NOW),
      tuple("defaultTemplate.app", "APP", NOW)
    );
  }

  @Test
  public void fail_when_default_organization_is_missing() {
    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class)
    .hasMessage("Default organization is missing");
  }

  private void assertInternalProperties(Tuple... tuples) {
    assertThat(db.select("select kee, text_value, created_at from internal_properties"))
      .extracting(m -> m.get("KEE"), m -> m.get("TEXT_VALUE"), m -> m.get("CREATED_AT"))
      .containsExactlyInAnyOrder(tuples);
  }

  private void insertDefaultOrganization(@Nullable String defaultPermissionTemplateForProject, @Nullable String defaultPermissionTemplateForPortfolio,
    @Nullable String defaultPermissionTemplateForApplication) {
    String uuid = uuidFactory.create();
    db.executeInsert("organizations",
      "uuid", uuid,
      "kee", "default-organization",
      "name", "name" + uuid,
      "default_perm_template_project", defaultPermissionTemplateForProject,
      "default_perm_template_port", defaultPermissionTemplateForPortfolio,
      "default_perm_template_app", defaultPermissionTemplateForApplication,
      "default_quality_gate_uuid", uuid,
      "new_project_private", true,
      "subscription", uuid,
      "created_at", NOW,
      "updated_at", NOW);
  }

}
