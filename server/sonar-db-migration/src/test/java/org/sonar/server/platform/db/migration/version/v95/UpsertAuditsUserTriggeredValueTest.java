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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class UpsertAuditsUserTriggeredValueTest {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpsertAuditsUserTriggeredValueTest.class, "schema.sql");

  private final DataChange underTest = new UpsertAuditsUserTriggeredValue(db.database());

  @Test
  public void migration_populates_audits_user_triggered() throws SQLException {
    String auditUuid1 = insertUserTriggeredLog();
    String auditUuid2 = insertSystemTriggeredLog();

    underTest.execute();

    assertUserTriggeredIsTrue(auditUuid1);
    assertUserTriggeredIsFalse(auditUuid2);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String auditUuid1 = insertUserTriggeredLog();
    String auditUuid2 = insertSystemTriggeredLog();

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertUserTriggeredIsTrue(auditUuid1);
    assertUserTriggeredIsFalse(auditUuid2);
  }

  private void assertUserTriggeredIsTrue(String userUuid) {
    assertUserTriggered(userUuid,true);
  }

  private void assertUserTriggeredIsFalse(String userUuid) {
    assertUserTriggered(userUuid,false);
  }

  private void assertUserTriggered(String userUuid, boolean userTriggered) {
    String selectSql = String.format("select USER_TRIGGERED from audits where uuid='%s'", userUuid);
    assertThat(db.select(selectSql).stream().map(row -> row.get("USER_TRIGGERED")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder(userTriggered);
  }

  private String insertUserTriggeredLog() {
    Map<String, Object> map = getAuditsValueMap(uuidFactory.create(), randomAlphabetic(20));
    return insertAuditsValueMap(map);
  }

  private String insertSystemTriggeredLog() {
    Map<String, Object> map = getAuditsValueMap("-", "System");
    return insertAuditsValueMap(map);
  }

  @NotNull
  private Map<String, Object> getAuditsValueMap(String userUuid, String userLogin) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("USER_UUID", userUuid);
    map.put("USER_LOGIN", userLogin);
    map.put("CATEGORY", "USER");
    map.put("OPERATION", "UPDATE");
    map.put("CREATED_AT", System.currentTimeMillis());
    map.put("NEW_VALUE", "Some Value");
    map.put("USER_TRIGGERED", true);
    return map;
  }

  private String insertAuditsValueMap(Map<String, Object> map) {
    db.executeInsert("audits", map);
    return (String) map.get("UUID");
  }

}
