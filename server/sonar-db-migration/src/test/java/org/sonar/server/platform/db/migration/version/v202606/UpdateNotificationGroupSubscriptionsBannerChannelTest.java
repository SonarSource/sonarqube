/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateNotificationGroupSubscriptionsBannerChannelTest {

  private static final String LEGACY_CHANNEL_KEY = "SecurityAlertBannerChannel";
  private static final String BANNER_CHANNEL_KEY = "BannerChannel";
  private static final String TABLE_NAME = "notif_group_subscriptions";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(UpdateNotificationGroupSubscriptionsBannerChannel.class);

  private final UpdateNotificationGroupSubscriptionsBannerChannel underTest =
    new UpdateNotificationGroupSubscriptionsBannerChannel(db.database());

  @Test
  void execute_shouldRenameLegacyBannerChannelAndLeaveOtherChannelsUnchanged() throws SQLException {
    db.executeInsert(TABLE_NAME,
      "uuid", "legacy-subscription",
      "group_uuid", "group-1",
      "notification_type", "security-alert-raised",
      "channel_key", LEGACY_CHANNEL_KEY,
      "created_at", 1L);
    db.executeInsert(TABLE_NAME,
      "uuid", "email-subscription",
      "group_uuid", "group-1",
      "notification_type", "security-alert-raised",
      "channel_key", "EmailNotificationChannel",
      "created_at", 1L);

    underTest.execute();
    underTest.execute();

    assertThat(db.selectFirst("SELECT channel_key FROM " + TABLE_NAME + " WHERE uuid = 'legacy-subscription'"))
      .containsEntry("CHANNEL_KEY", BANNER_CHANNEL_KEY);
    assertThat(db.selectFirst("SELECT channel_key FROM " + TABLE_NAME + " WHERE uuid = 'email-subscription'"))
      .containsEntry("CHANNEL_KEY", "EmailNotificationChannel");
  }
}
