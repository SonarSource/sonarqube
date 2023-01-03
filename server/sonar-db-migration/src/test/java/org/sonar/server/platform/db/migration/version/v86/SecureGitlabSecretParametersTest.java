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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SecureGitlabSecretParametersTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SecureGitlabSecretParametersTest.class, "schema.sql");

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private final DataChange underTest = new SecureGitlabSecretParameters(db.database());

  @Test
  public void secure_gitlab_secret_parameters() throws SQLException {
    insertGitlabProperties();

    underTest.execute();

    assertThat(db.select("select * from PROPERTIES"))
      .extracting(r -> r.get("PROP_KEY"), r -> r.get("TEXT_VALUE"))
      .containsExactlyInAnyOrder(
        tuple("sonar.auth.gitlab.secret.secured", "secret secret"),
        tuple("sonar.auth.gitlab.applicationId.secured", "secret applicationId"));
  }

  private void insertGitlabProperties() {
    db.executeInsert("PROPERTIES",
      "prop_key", "sonar.auth.gitlab.secret",
      "is_empty", false,
      "text_value", "secret secret",
      "uuid", uuidFactory.create(),
      "created_at", System2.INSTANCE.now());
    db.executeInsert("PROPERTIES",
      "prop_key", "sonar.auth.gitlab.applicationId",
      "is_empty", false,
      "text_value", "secret applicationId",
      "uuid", uuidFactory.create(),
      "created_at", System2.INSTANCE.now());
  }

}
