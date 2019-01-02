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
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateTmpLastKeyColumnsToCeActivityTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateTmpLastKeyColumnsToCeActivityTest.class, "ce_activity.sql");

  private PopulateTmpLastKeyColumnsToCeActivity underTest = new PopulateTmpLastKeyColumnsToCeActivity(db.database());

  @Test
  public void execute_has_no_effect_on_empty_table() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("ce_activity")).isZero();
  }

  @Test
  public void execute_populate_tmp_last_key_columns_from_type_and_component_uuid_columns() throws SQLException {
    String type = randomAlphabetic(6);
    String oldComponentUuid = randomAlphabetic(7);
    String tmpComponentUuid = randomAlphabetic(8);
    String tmpMainComponentUuid = randomAlphabetic(9);

    String taskWithComponentUuid = insertCeActivity(type, oldComponentUuid, tmpComponentUuid, tmpMainComponentUuid);
    String taskWithInconsistentComponentUuid = insertCeActivity(type, null, tmpComponentUuid, tmpMainComponentUuid);
    String taskNoComponentUuid = insertCeActivity(type, null, null, null);

    underTest.execute();

    assertThatTmpLastKeyAndMainLastKeyOf(taskWithComponentUuid).containsOnly(tuple(type + tmpComponentUuid, type + tmpMainComponentUuid));
    assertThatTmpLastKeyAndMainLastKeyOf(taskWithInconsistentComponentUuid).containsOnly(tuple(type, type));
    assertThatTmpLastKeyAndMainLastKeyOf(taskNoComponentUuid).containsOnly(tuple(type, type));

    assertThatTmpIsLastAndMainIsLastOf(taskWithComponentUuid).containsOnly(tuple(false, false));
    assertThatTmpIsLastAndMainIsLastOf(taskWithInconsistentComponentUuid).containsOnly(tuple(false, false));
    assertThatTmpIsLastAndMainIsLastOf(taskNoComponentUuid).containsOnly(tuple(false, false));
  }
  @Test
  public void execute_is_reentrant() throws SQLException {
    execute_populate_tmp_last_key_columns_from_type_and_component_uuid_columns();

    underTest.execute();
  }

  private String insertCeActivity(String type,
    @Nullable String oldComponentUuid,
    @Nullable String tmpComponentUuid, @Nullable String tmpMainComponentUuid) {
    checkArgument((tmpComponentUuid == null) == (tmpMainComponentUuid == null));

    String uuid = UuidFactoryFast.getInstance().create();

    Random random = new Random();
    db.executeInsert(
      "ce_activity",
      "UUID", uuid,
      "TASK_TYPE", type,
      "COMPONENT_UUID", oldComponentUuid,
      "TMP_COMPONENT_UUID", tmpComponentUuid,
      "TMP_MAIN_COMPONENT_UUID", tmpMainComponentUuid,
      "STATUS", randomAlphabetic(5),
      "IS_LAST", random.nextBoolean(),
      "IS_LAST_KEY", randomAlphabetic(12),
      "EXECUTION_COUNT", random.nextInt(10),
      "SUBMITTED_AT", (long) random.nextInt(5_999),
      "CREATED_AT", (long) random.nextInt(5_999),
      "UPDATED_AT", (long) random.nextInt(5_999));

    return uuid;
  }

  private AbstractIterableAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatTmpLastKeyAndMainLastKeyOf(String uuid) {
    return assertThat(db.select("select tmp_is_last_key as \"LAST_KEY\", tmp_main_is_last_key as \"MAIN_LAST_KEY\" from ce_activity where uuid='" + uuid + "'"))
      .extracting(t -> (String) t.get("LAST_KEY"), t -> (String) t.get("MAIN_LAST_KEY"));
  }

  private AbstractIterableAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatTmpIsLastAndMainIsLastOf(String uuid) {
    return assertThat(db.select("select tmp_is_last as \"LAST\", tmp_main_is_last as \"MAIN_LAST\" from ce_activity where uuid='" + uuid + "'"))
      .extracting(t -> (Boolean) t.get("LAST"), t -> (Boolean) t.get("MAIN_LAST"));
  }
}
