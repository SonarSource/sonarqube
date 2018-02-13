/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProviderImpl;

import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class MigrateWebhooksToWebhooksTableTest {

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(MigrateWebhooksToWebhooksTableTest.class, "migrate_webhooks.sql");
  private static final long NOW = 1_500_000_000_000L;
  private System2 system2 = new TestSystem2().setNow(NOW);
  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private MigrateWebhooksToWebhooksTable underTest = new MigrateWebhooksToWebhooksTable(dbTester.database(), new DefaultOrganizationUuidProviderImpl(), uuidFactory);

  @Test
  public void should_do_nothing_if_no_webhooks() throws SQLException {

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(0);
  }

  @Test
  public void should_migrate_one_global_webhook() throws SQLException {
    String uuid = insertDefaultOrganization();
    insertProperty("sonar.webhooks.global", "1", null, system2.now());
    insertProperty("sonar.webhooks.global.1.name", "a webhook", null, system2.now());
    insertProperty("sonar.webhooks.global.1.url", "http://webhook.com", null, system2.now());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(1);

    Map<String, Object> migrated = dbTester.selectFirst("select * from webhooks");
    assertThat(migrated.get("UUID")).isNotNull();
    assertThat(migrated.get("NAME")).isEqualTo("a webhook");
    assertThat(migrated.get("URL")).isEqualTo("http://webhook.com");
    assertThat(migrated.get("PROJECT_UUID")).isNull();
    assertThat(migrated.get("ORGANIZATION_UUID")).isEqualTo(uuid);
    assertThat(migrated.get("URL")).isEqualTo("http://webhook.com");
    assertThat(migrated.get("CREATED_AT")).isEqualTo(system2.now());
    assertThat(migrated.get("UPDATED_AT")).isEqualTo(system2.now());
  }

  @Test
  public void should_migrate_one_project_webhook() throws SQLException {
    String organization = insertDefaultOrganization();
    String projectId = "156";
    String projectUuid = UuidFactoryFast.getInstance().create();
    ;
    insertProject(organization, projectId, projectUuid);

    insertProperty("sonar.webhooks.project", "1", projectId, system2.now());
    insertProperty("sonar.webhooks.project.1.name", "a webhook", projectId, system2.now());
    insertProperty("sonar.webhooks.project.1.url", "http://webhook.com", projectId, system2.now());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(1);

    Map<String, Object> migrated = dbTester.selectFirst("select * from webhooks");
    assertThat(migrated.get("UUID")).isNotNull();
    assertThat(migrated.get("NAME")).isEqualTo("a webhook");
    assertThat(migrated.get("URL")).isEqualTo("http://webhook.com");
    assertThat(migrated.get("PROJECT_UUID")).isEqualTo(projectUuid);
    assertThat(migrated.get("ORGANIZATION_UUID")).isNull();
    assertThat(migrated.get("URL")).isEqualTo("http://webhook.com");
    assertThat(migrated.get("CREATED_AT")).isEqualTo(system2.now());
    assertThat(migrated.get("UPDATED_AT")).isEqualTo(system2.now());
  }

  @Test
  public void should_migrate_global_webhooks() throws SQLException {
    insertDefaultOrganization();
    insertProperty("sonar.webhooks.global", "1,2", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.1.name", "a webhook", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.1.url", "http://webhook.com", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.2.name", "a webhook", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.2.url", "http://webhook.com", null, parseLong(randomNumeric(7)));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(2);
  }

  @Test
  public void should_migrate_only_valid_webhooks() throws SQLException {
    insertDefaultOrganization();
    insertProperty("sonar.webhooks.global", "1,2,3,4", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.1.url", "http://webhook.com", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.2.name", "a webhook", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.3.name", "a webhook", null, parseLong(randomNumeric(7)));
    insertProperty("sonar.webhooks.global.3.url", "http://webhook.com", null, parseLong(randomNumeric(7)));
    // nothing for 4

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(1);
  }

  @Test
  public void should_migrate_project_webhooks() throws SQLException {
    String organization = insertDefaultOrganization();
    String projectId = "156";
    String projectUuid = UuidFactoryFast.getInstance().create();
    ;
    insertProject(organization, projectId, projectUuid);

    insertProperty("sonar.webhooks.project", "1,2", projectId, system2.now());
    insertProperty("sonar.webhooks.project.1.name", "a webhook", projectId, system2.now());
    insertProperty("sonar.webhooks.project.1.url", "http://webhook.com", projectId, system2.now());
    insertProperty("sonar.webhooks.project.2.name", "another webhook", projectId, system2.now());
    insertProperty("sonar.webhooks.project.2.url", "http://webhookhookhook.com", projectId, system2.now());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(2);
  }

  @Test
  public void should_not_migrate_more_than_10_webhooks_per_project() throws SQLException {

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(0);
  }

  @Test
  public void should_not_migrate_more_than_10_global_webhooks() throws SQLException {

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("properties")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(0);
  }

  private void insertProperty(String key, @Nullable String value, @Nullable String resourceId, Long date) {
    dbTester.executeInsert("PROPERTIES",
      "id", randomNumeric(7),
      "prop_key", valueOf(key),
      "text_value", value,
      "is_empty", value.isEmpty() ? true : false,
      "resource_id", resourceId == null ? null : valueOf(resourceId),
      "created_at", valueOf(date));
  }

  private String insertDefaultOrganization() {
    String uuid = UuidFactoryFast.getInstance().create();
    dbTester.executeInsert(
      "INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "IS_EMPTY", "false",
      "TEXT_VALUE", uuid);
    return uuid;
  }

  private void insertProject(String organizationUuid, String projectId, String projectUuid) {
    dbTester.executeInsert(
      "PROJECTS",
      "ID", projectId,
      "ORGANIZATION_UUID", organizationUuid,
      "UUID", projectUuid);
  }
}
