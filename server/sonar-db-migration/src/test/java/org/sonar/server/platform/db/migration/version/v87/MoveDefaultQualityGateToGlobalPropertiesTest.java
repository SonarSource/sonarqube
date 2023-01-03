/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v87;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MoveDefaultQualityGateToGlobalPropertiesTest {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final TestSystem2 system2 = new TestSystem2();
  private final long NOW = 1606375781L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MoveDefaultQualityGateToGlobalPropertiesTest.class, "schema.sql");

  private final MoveDefaultQualityGateToGlobalProperties underTest = new MoveDefaultQualityGateToGlobalProperties(db.database(),
    UuidFactoryFast.getInstance(), system2);

  @Before
  public void before() {
    system2.setNow(NOW);
  }

  @Test
  public void fail_if_organization_not_exist() {
    assertThatThrownBy(underTest::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default organization is missing");
  }

  @Test
  public void migrate_default_quality_gate_from_default_organization() throws SQLException {
    insertDefaultOrganization("default-quality-gate-uuid");

    underTest.execute();

    assertThatDefaultQualityGateIsEqualTo("default-quality-gate-uuid");
  }

  private void insertDefaultOrganization(String defaultQualityGate) {
    String uuid = uuidFactory.create();
    db.executeInsert("organizations",
      "uuid", uuid,
      "kee", "default-organization",
      "name", "name" + uuid,
      "default_perm_template_project", uuidFactory.create(),
      "default_perm_template_port", uuidFactory.create(),
      "default_perm_template_app", uuidFactory.create(),
      "default_quality_gate_uuid", defaultQualityGate,
      "new_project_private", false,
      "subscription", uuid,
      "created_at", NOW,
      "updated_at", NOW);
  }

  private void assertThatDefaultQualityGateIsEqualTo(String s) {
    assertThat(db.selectFirst("select p.text_value, p.created_at from properties p where p.prop_key = 'qualitygate.default'"))
      .containsEntry("TEXT_VALUE", s)
      .containsEntry("CREATED_AT", NOW);
  }
}
