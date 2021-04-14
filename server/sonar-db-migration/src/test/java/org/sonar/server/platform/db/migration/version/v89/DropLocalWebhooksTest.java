/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v89;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.version.v89.util.NetworkInterfaceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DropLocalWebhooksTest {

  @Rule
  public LogTester logTester = new LogTester();

  private static final String TABLE_NAME = "webhooks";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(DropLocalWebhooksTest.class, "schema.sql");

  private final NetworkInterfaceProvider networkInterfaceProvider = mock(NetworkInterfaceProvider.class);

  private final DataChange underTest = new DropLocalWebhooks(dbTester.database(), networkInterfaceProvider);

  @Before
  public void prepare() throws IOException {
    InetAddress inetAddress1 = InetAddress.getByName(HttpUrl.parse("https://0.0.0.0/some_webhook").host());
    InetAddress inetAddress2 = InetAddress.getByName(HttpUrl.parse("https://127.0.0.1/some_webhook").host());
    InetAddress inetAddress3 = InetAddress.getByName(HttpUrl.parse("https://localhost/some_webhook").host());

    when(networkInterfaceProvider.getNetworkInterfaceAddresses())
      .thenReturn(ImmutableList.of(inetAddress1, inetAddress2, inetAddress3));
  }

  @Test
  public void execute() throws SQLException {
    prepareWebhooks();

    underTest.execute();

    verifyMigrationResult();
  }

  @Test
  public void migrationIsReEntrant() throws SQLException {
    prepareWebhooks();

    underTest.execute();
    underTest.execute();

    verifyMigrationResult();
  }

  @Test
  public void migrationIsSuccessfulWhenNoWebhooksDeleted() throws SQLException {
    insertProject("p1", "pn1");
    insertWebhook("uuid-1", "https://10.15.15.15:5555/some_webhook", "p1");
    insertWebhook("uuid-5", "https://some.valid.address.com/random_webhook", null);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_NAME)).isEqualTo(2);
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void migrationIsSuccessfulWhenNoWebhooksInDb() throws SQLException {
    insertProject("p1", "pn1");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_NAME)).isZero();
    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  private void prepareWebhooks() {
    insertProject("p1", "pn1");
    insertProject("p2", "pn2");
    insertWebhook("uuid-1", "https://10.15.15.15:5555/some_webhook", "p1");
    insertWebhook("uuid-2", "https://0.0.0.0/some_webhook", "p1");
    insertWebhook("uuid-3", "https://172.16.16.16:6666/some_webhook", "p2");
    insertWebhook("uuid-4", "https://127.0.0.1/some_webhook", "p2");
    insertWebhook("uuid-5", "https://some.valid.address.com/random_webhook", null);
    insertWebhook("uuid-6", "https://248.235.76.254:7777/some_webhook", null);
    insertWebhook("uuid-7", "https://localhost/some_webhook", null);
  }

  private void verifyMigrationResult() {
    assertThat(dbTester.countRowsOfTable(TABLE_NAME)).isEqualTo(4);
    assertThat(dbTester.select("select uuid from " + TABLE_NAME).stream().map(columns -> columns.get("UUID")))
      .containsOnly("uuid-1", "uuid-3", "uuid-5", "uuid-6");

    List<String> logs = logTester.logs(LoggerLevel.WARN);
    assertThat(logs).hasSize(3);
    assertThat(logs).containsExactlyInAnyOrder(
      "Global webhook 'webhook-uuid-7' has been removed because it used an invalid, unsafe URL. Please recreate this webhook with a valid URL if it is still needed.",
      "Webhook 'webhook-uuid-4' for project 'pn2' has been removed because it used an invalid, unsafe URL. Please recreate this webhook with a valid URL or ask a project administrator to do it if it is still needed.",
      "Webhook 'webhook-uuid-2' for project 'pn1' has been removed because it used an invalid, unsafe URL. Please recreate this webhook with a valid URL or ask a project administrator to do it if it is still needed.");
  }

  private void insertProject(String uuid, String name) {
    dbTester.executeInsert("PROJECTS",
      "NAME", name,
      "ORGANIZATION_UUID", "default",
      "KEE", uuid + "-key",
      "UUID", uuid,
      "PRIVATE", Boolean.toString(false),
      "QUALIFIER", "TRK",
      "UPDATED_AT", System2.INSTANCE.now());
  }

  private void insertWebhook(String uuid, String url, @Nullable String projectUuid) {
    dbTester.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "NAME", "webhook-" + uuid,
      "PROJECT_UUID", projectUuid,
      "URL", url,
      "CREATED_AT", System2.INSTANCE.now());
  }
}

