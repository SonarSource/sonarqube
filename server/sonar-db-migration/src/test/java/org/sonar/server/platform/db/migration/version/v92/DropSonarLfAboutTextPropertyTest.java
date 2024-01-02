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
package org.sonar.server.platform.db.migration.version.v92;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class DropSonarLfAboutTextPropertyTest {

  private final UuidFactory uuidFactory = new SequenceUuidFactory();

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DropSonarLfAboutTextPropertyTest.class, "schema.sql");

  private final DataChange underTest = new DropSonarLfAboutTextPropertyValue(db.database());

  @Test
  public void do_not_fail_if_no_rows_to_delete() {
    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();
  }

  @Test
  public void deletes_lf_about_text_property_when_they_exist_in_database() throws SQLException {
    insertProperty(DropSonarLfAboutTextPropertyValue.PROPERTY_TO_DELETE_KEY, "This is an about text test sample");

    underTest.execute();

    assertThat(db.countSql(
      String.format("select count(*) from properties where prop_key = '%s'",
        DropSonarLfAboutTextPropertyValue.PROPERTY_TO_DELETE_KEY
      ))).isZero();
  }

  private void insertProperty(String key, String value) {
    db.executeInsert("properties",
      "PROP_KEY", key,
      "TEXT_VALUE", value,
      "IS_EMPTY", String.valueOf(false),
      "CREATED_AT", 2,
      "UUID", uuidFactory.create());
  }
}
