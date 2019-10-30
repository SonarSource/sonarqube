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

package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import java.util.Base64;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class MigrateDeprecatedGithubPrivateKeyToNewKeyTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateDeprecatedGithubPrivateKeyToNewKeyTest.class, "schema.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DataChange underTest = new MigrateDeprecatedGithubPrivateKeyToNewKey(db.database());

  @Test
  public void migrate_and_decode_old_property_to_new_one() throws SQLException {
    String privateKey = "<PRIVATE_KEY>";
    insertProperty("sonar.alm.github.app.privateKey.secured", Base64.getEncoder().encodeToString(privateKey.getBytes()));

    underTest.execute();

    assertProperties(tuple("sonar.alm.github.app.privateKeyContent.secured", privateKey));
  }

  @Test
  public void do_nothing_when_only_new_property() throws SQLException {
    String privateKey = "<PRIVATE_KEY>";
    insertProperty("sonar.alm.github.app.privateKeyContent.secured", privateKey);

    underTest.execute();

    assertProperties(tuple("sonar.alm.github.app.privateKeyContent.secured", privateKey));
  }

  @Test
  public void remove_old_property_when_both_new_one_and_old_one_exist() throws SQLException {
    String privateKey = "<PRIVATE_KEY>";
    insertProperty("sonar.alm.github.app.privateKey.secured", Base64.getEncoder().encodeToString(privateKey.getBytes()));
    insertProperty("sonar.alm.github.app.privateKeyContent.secured", privateKey);

    underTest.execute();

    assertProperties(tuple("sonar.alm.github.app.privateKeyContent.secured", privateKey));
  }

  @Test
  public void migration_is_re_entrant() throws SQLException {
    String privateKey = "<PRIVATE_KEY>";
    insertProperty("sonar.alm.github.app.privateKey.secured", Base64.getEncoder().encodeToString(privateKey.getBytes()));

    underTest.execute();
    underTest.execute();

    assertProperties(tuple("sonar.alm.github.app.privateKeyContent.secured", privateKey));
  }

  private void assertProperties(Tuple... tuples) {
    assertThat(db.select("SELECT prop_key, text_value FROM properties")
      .stream()
      .map(map -> new Tuple(map.get("PROP_KEY"), map.get("TEXT_VALUE")))
      .collect(toList()))
      .containsExactlyInAnyOrder(tuples);
  }

  private void insertProperty(String key, String value) {
    db.executeInsert(
      "PROPERTIES",
      "PROP_KEY", key,
      "RESOURCE_ID", null,
      "USER_ID", null,
      "IS_EMPTY", false,
      "TEXT_VALUE", value,
      "CLOB_VALUE", null,
      "CREATED_AT", System2.INSTANCE.now());
  }

}
