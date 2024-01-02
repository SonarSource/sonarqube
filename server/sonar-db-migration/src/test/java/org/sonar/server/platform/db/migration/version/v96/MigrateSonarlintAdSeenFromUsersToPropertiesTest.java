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
package org.sonar.server.platform.db.migration.version.v96;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v96.MigrateSonarlintAdSeenFromUsersToProperties.USER_DISMISSED_NOTICES_SONARLINT_AD;

public class MigrateSonarlintAdSeenFromUsersToPropertiesTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(MigrateSonarlintAdSeenFromUsersToPropertiesTest.class, "schema.sql");

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private final System2 system2 = new System2();

  private final DataChange underTest = new MigrateSonarlintAdSeenFromUsersToProperties(db.database(), uuidFactory, system2);

  @Test
  public void migrate_sonarlintAd_to_properties() throws SQLException {
    insertUser(db, "uuid-user-1", "user1", "externalId1", "externalLogin1", true);
    insertUser(db, "uuid-user-2", "user2", "externalId2", "externalLogin2", false);

    underTest.execute();

    assertThat(db.countSql("select count(*) from properties where prop_key='" + USER_DISMISSED_NOTICES_SONARLINT_AD + "' and user_uuid='uuid-user-1'")).isEqualTo(1);
    assertThat(db.countSql("select count(*) from properties where prop_key='" + USER_DISMISSED_NOTICES_SONARLINT_AD + "' and user_uuid='uuid-user-2'")).isZero();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertUser(db, "uuid-user-1", "user1", "externalId1", "externalLogin1", true);

    underTest.execute();
    underTest.execute();

    assertThat(db.countSql("select count(*) from properties where prop_key='" + USER_DISMISSED_NOTICES_SONARLINT_AD + "' and user_uuid='uuid-user-1'")).isEqualTo(1);
  }

  private void insertUser(CoreDbTester db, String userUuid, String login, String externalId, String externalLogin, boolean seen) {
    db.executeInsert("users", "UUID", userUuid,
      "login", login,
      "external_identity_provider", "none",
      "external_id", externalId,
      "external_login", externalLogin,
      "reset_password", false,
      "sonarlint_ad_seen", seen
    );
  }


}