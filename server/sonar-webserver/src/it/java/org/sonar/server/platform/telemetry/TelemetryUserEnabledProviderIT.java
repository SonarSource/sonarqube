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
package org.sonar.server.platform.telemetry;

import java.util.Map;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.util.DigestUtil;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryUserEnabledProviderIT {

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public final DbTester db = DbTester.create(system2);


  private final TelemetryUserEnabledProvider underTest = new TelemetryUserEnabledProvider(db.getDbClient());

  @BeforeEach
  public void beforeEach() {
    db.executeUpdateSql("delete from users");
  }

  @Test
  void getValues_whenNoUsersInDatabase_shouldReturnEmptyMap() {
    Map<String, Boolean> uuidValues = underTest.getValues();

    assertThat(uuidValues).isEmpty();
  }

  @Test
  void getValues_whenSomeUsersActive_shouldReturnBothBooleanValues() {
    db.users().insertUser(user -> user.setUuid("uuid1").setActive(true));
    db.users().insertUser(user -> user.setUuid("uuid1").setActive(false));
    db.getSession().commit();

    Map<String, Boolean> uuidValues = underTest.getValues();

    assertThat(uuidValues).hasSize(2);
    assertThat(uuidValues.values().stream().filter(Boolean::booleanValue)).hasSize(1);
    assertThat(uuidValues.values().stream().filter(b -> !b)).hasSize(1);
  }

  @Test
  void getValues_when10ActiveUsers_shouldReturn10BooleanValues() {
    for (int i = 0; i < 10; i++) {
      db.users().insertUser(user -> user.setActive(true));
    }
    db.getSession().commit();

    Map<String, Boolean> uuidValues = underTest.getValues();

    assertThat(uuidValues).hasSize(10);
    assertThat(uuidValues.values().stream().filter(Boolean::booleanValue)).hasSize(10);
  }

  @Test
  void getValues_shouldAnonymizeUserUuids() {
    UserDto userDto1 = db.users().insertUser();
    UserDto userDto2 = db.users().insertUser();
    db.getSession().commit();

    Map<String, Boolean> uuidValues = underTest.getValues();

    String anonymizedUser1 = DigestUtil.sha3_224Hex(userDto1.getUuid());
    String anonymizedUser2 = DigestUtil.sha3_224Hex(userDto2.getUuid());
    assertThat(uuidValues.keySet()).containsExactlyInAnyOrder(anonymizedUser1, anonymizedUser2);
  }

}
