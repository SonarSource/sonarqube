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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class UpsertSonarlintAdSeenValueTest {

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpsertSonarlintAdSeenValueTest.class, "schema.sql");

  private final DataChange underTest = new UpsertSonarlintAdSeenValue(db.database());

  @Test
  public void migration_populates_sonarlint_promotion_seen_based_on_last_sonarlint_connection() throws SQLException {
    String userUuid1 = insertUserWithLastSonarlintConnection(true);
    String userUuid2 = insertUserWithLastSonarlintConnection(false);

    underTest.execute();

    assertSonarlintPromotionSeenIsUpsertedCorrectly(userUuid1, true);
    assertSonarlintPromotionSeenIsUpsertedCorrectly(userUuid2, false);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    String userUuid1 = insertUserWithLastSonarlintConnection(true);
    String userUuid2 = insertUserWithLastSonarlintConnection(false);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertSonarlintPromotionSeenIsUpsertedCorrectly(userUuid1, true);
    assertSonarlintPromotionSeenIsUpsertedCorrectly(userUuid2, false);
  }

  private void assertSonarlintPromotionSeenIsUpsertedCorrectly(String userUuid, boolean seen) {
    String selectSql = String.format("select SONARLINT_AD_SEEN from users where uuid='%s'", userUuid);
    assertThat(db.select(selectSql).stream().map(row -> row.get("SONARLINT_AD_SEEN")).collect(Collectors.toList()))
      .containsExactlyInAnyOrder(seen);
  }

  private String insertUserWithLastSonarlintConnection(boolean setLastSonarlintConnection) {
    Map<String, Object> map = new HashMap<>();
    String uuid = uuidFactory.create();
    map.put("UUID", uuid);
    map.put("LOGIN", randomAlphabetic(20));
    map.put("EXTERNAL_LOGIN", randomAlphabetic(20));
    map.put("EXTERNAL_IDENTITY_PROVIDER", "sonarqube");
    map.put("EXTERNAL_ID", randomNumeric(5));
    map.put("IS_ROOT", false);
    map.put("ONBOARDED", false);
    map.put("CREATED_AT", System.currentTimeMillis());
    if (setLastSonarlintConnection) {
      map.put("LAST_SONARLINT_CONNECTION", System.currentTimeMillis());
    }
    map.put("RESET_PASSWORD", false);
    db.executeInsert("users", map);

    return uuid;
  }
}
