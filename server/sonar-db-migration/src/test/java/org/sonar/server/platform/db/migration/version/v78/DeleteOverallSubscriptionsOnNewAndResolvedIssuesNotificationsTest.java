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
package org.sonar.server.platform.db.migration.version.v78;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteOverallSubscriptionsOnNewAndResolvedIssuesNotificationsTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteOverallSubscriptionsOnNewAndResolvedIssuesNotificationsTest.class, "schema.sql");

  private DataChange underTest = new DeleteOverallSubscriptionsOnNewAndResolvedIssuesNotifications(db.database());

  @Test
  public void delete_overall_subscriptions() throws SQLException {
    // properties that do not relate to subscriptions
    insertProperty(1, "foo", null);
    insertProperty(2, "bar", nextInt());

    // global subscriptions to be deleted
    insertProperty(3, "notification.NewFalsePositiveIssue.EmailNotificationChannel", null);
    insertProperty(4, "notification.NewFalsePositiveIssue.OtherChannel", null);
    insertProperty(5, "notification.NewIssues.EmailNotificationChannel", null);
    insertProperty(6, "notification.NewIssues.OtherChannel", null);

    // global subscriptions on other notifications, to be kept
    insertProperty(7, "notification.ChangesOnMyIssue.EmailNotificationChannel", null);
    insertProperty(8, "notification.NewAlerts.EmailNotificationChannel", null);
    insertProperty(9, "notification.CeReportTaskFailure.EmailNotificationChannel", null);
    insertProperty(10, "notification.SQ-MyNewIssues.EmailNotificationChannel", null);

    // project subscriptions, to be kept
    insertProperty(11, "notification.NewFalsePositiveIssue.EmailNotificationChannel", nextInt());
    insertProperty(12, "notification.NewIssues.EmailNotificationChannel", nextInt());
    insertProperty(13, "notification.ChangesOnMyIssue.EmailNotificationChannel", nextInt());
    insertProperty(14, "notification.NewAlerts.EmailNotificationChannel", nextInt());
    insertProperty(15, "notification.CeReportTaskFailure.EmailNotificationChannel", nextInt());
    insertProperty(16, "notification.SQ-MyNewIssues.EmailNotificationChannel", nextInt());

    underTest.execute();

    assertProperties(1, 2, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
  }

  private void assertProperties(Integer... expectedIds) {
    assertThat(db.select("SELECT id FROM properties")
      .stream()
      .map(map -> ((Long) map.get("ID")).intValue())
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedIds);
  }

  private void insertProperty(int id, String key, @Nullable Integer componentId) {
    db.executeInsert(
      "PROPERTIES",
      "ID", id,
      "PROP_KEY", key,
      "USER_ID", nextInt(),
      "RESOURCE_ID", componentId,
      "IS_EMPTY", true,
      "CREATED_AT", 123456);
  }
}
