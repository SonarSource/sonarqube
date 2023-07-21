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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateCreatedAtTempInComponentsTest {

  private static final String TABLE_NAME = "components";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(PopulateCreatedAtTempInComponentsTest.class, "schema.sql");

  private final PopulateCreatedAtTempInComponents underTest = new PopulateCreatedAtTempInComponents(db.database());

  @Test
  public void execute_whenComponentsDoNotExist_shouldNotFail() {
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  @Test
  public void execute_whenComponentsExist_shouldPopulateColumn() throws SQLException, ParseException {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    format.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date date1 = format.parse("2023-01-01 10:22:38.222");
    Date date2 = format.parse("2023-07-20 02:00:01.800");

    insertComponent("uuid-1", null);
    insertComponent("uuid-2", date1);
    insertComponent("uuid-3", date2);

    underTest.execute();

    assertThat(db.select("select UUID, CREATED_AT_TEMP from components"))
      .extracting(stringObjectMap -> stringObjectMap.get("UUID"), stringObjectMap -> stringObjectMap.get("CREATED_AT_TEMP"))
      .containsExactlyInAnyOrder(
        tuple("uuid-1", null),
        tuple("uuid-2", 1672568558222L),
        tuple("uuid-3", 1689818401800L));
  }

  private void insertComponent(String uuid, @Nullable Date createdAt) {
    db.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "UUID_PATH", "P",
      "BRANCH_UUID", "B",
      "ENABLED", true,
      "PRIVATE", true,
      "CREATED_AT", createdAt,
      "CREATED_AT_TEMP", null);
  }
}
