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
package org.sonar.server.platform.db.migration.version.v71;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProviderImpl;

import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MigrateWebhooksToWebhooksTableTest {
  private static final long NOW = 1_500_000_000_000L;
  private static final boolean ENABLED = true;
  private static final boolean DISABLED = false;

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(MigrateWebhooksToWebhooksTableTest.class, "migrate_webhooks.sql");

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private MigrateWebhooksToWebhooksTable underTest = new MigrateWebhooksToWebhooksTable(dbTester.database(), new DefaultOrganizationUuidProviderImpl(), uuidFactory);

  @Test
  public void should_do_nothing_if_no_webhooks() throws SQLException {
    underTest.execute();

    assertNoMoreWebhookProperties();
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(0);
  }

  @Test
  @UseDataProvider("numberOfGlobalWebhooksToMigration")
  public void execute_migrates_any_number_of_global_webhook_to_default_organization(int numberOfGlobalWebhooks) throws SQLException {
    String defaultOrganizationUuid = insertDefaultOrganization();
    insertGlobalWebhookProperties(numberOfGlobalWebhooks);
    Row[] webhooks = IntStream.range(1, numberOfGlobalWebhooks + 1)
      .mapToObj(i -> insertGlobalWebhookProperty(i, "name webhook " + i, "url webhook " + i, defaultOrganizationUuid))
      .map(Row::new)
      .toArray(Row[]::new);

    underTest.execute();

    assertThat(selectWebhooksInDb())
      .containsOnly(webhooks)
      .extracting(Row::getUuid)
      .doesNotContainNull();
    assertNoMoreWebhookProperties();
  }

  @DataProvider
  public static Object[][] numberOfGlobalWebhooksToMigration() {
    return new Object[][] {
      {1},
      {2},
      {2 + new Random().nextInt(10)}
    };
  }

  @Test
  public void execute_deletes_inconsistent_properties_for_global_webhook() throws SQLException {
    String defaultOrganizationUuid = insertDefaultOrganization();
    insertGlobalWebhookProperties(4);
    insertGlobalWebhookProperty(1, null, "no name", defaultOrganizationUuid);
    insertGlobalWebhookProperty(2, "no url", null, defaultOrganizationUuid);
    insertGlobalWebhookProperty(3, null, null, defaultOrganizationUuid);
    Webhook webhook = insertGlobalWebhookProperty(4, "name", "url", defaultOrganizationUuid);

    underTest.execute();

    assertThat(selectWebhooksInDb()).containsOnly(new Row(webhook));
    assertNoMoreWebhookProperties();
  }

  @Test
  @UseDataProvider("DP_execute_migrates_any_number_of_webhooks_for_any_number_of_existing_project")
  public void execute_migrates_any_number_of_webhooks_for_any_number_of_existing_project(int webhookCount, int projectCount) throws SQLException {
    Project[] projects = IntStream.range(0, projectCount)
      .mapToObj(i -> insertProject(ENABLED))
      .toArray(Project[]::new);
    Row[] rows = Arrays.stream(projects).flatMap(project -> {
      insertProjectWebhookProperties(project, webhookCount);
      return IntStream.range(1, webhookCount + 1)
        .mapToObj(i -> insertProjectWebhookProperty(project, i, "name webhook " + i, "url webhook " + i))
        .map(Row::new);
    }).toArray(Row[]::new);

    underTest.execute();

    assertThat(selectWebhooksInDb()).containsOnly(rows);
    assertNoMoreWebhookProperties();
  }

  @DataProvider
  public static Object[][] DP_execute_migrates_any_number_of_webhooks_for_any_number_of_existing_project() {
    Random random = new Random();
    return new Object[][] {
      {1, 1},
      {2, 1},
      {1, 2},
      {2 + random.nextInt(5), 2 + random.nextInt(5)}
    };
  }

  @Test
  public void execute_delete_webhooks_of_non_existing_project() throws SQLException {
    Project project = insertProject(ENABLED);
    Project nonExistingProject = new Project(233, "foo");
    Row[] rows = Stream.of(project, nonExistingProject)
      .map(prj -> {
        insertProjectWebhookProperties(prj, 1);
        return insertProjectWebhookProperty(prj, 1, "name", "url");
      })
      .map(Row::new)
      .toArray(Row[]::new);

    underTest.execute();

    assertThat(selectWebhooksInDb())
      .containsOnly(Arrays.stream(rows).filter(r -> Objects.equals(r.projectUuid, project.uuid)).toArray(Row[]::new));
    assertNoMoreWebhookProperties();
  }

  @Test
  public void execute_delete_webhooks_of_disabled_project() throws SQLException {
    Project project = insertProject(ENABLED);
    Project nonExistingProject = insertProject(DISABLED);
    Row[] rows = Stream.of(project, nonExistingProject)
      .map(prj -> {
        insertProjectWebhookProperties(prj, 1);
        return insertProjectWebhookProperty(prj, 1, "name", "url");
      })
      .map(Row::new)
      .toArray(Row[]::new);

    underTest.execute();

    assertThat(selectWebhooksInDb())
      .containsOnly(Arrays.stream(rows).filter(r -> Objects.equals(r.projectUuid, project.uuid)).toArray(Row[]::new));
    assertNoMoreWebhookProperties();
  }

  @Test
  public void execute_deletes_inconsistent_properties_for_project_webhook() throws SQLException {
    Project project = insertProject(ENABLED);
    insertProjectWebhookProperties(project, 4);
    insertProjectWebhookProperty(project, 1, null, "no name");
    insertProjectWebhookProperty(project, 2, "no url", null);
    insertProjectWebhookProperty(project, 3, null, null);
    Webhook webhook = insertProjectWebhookProperty(project, 4, "name", "url");

    underTest.execute();

    assertThat(selectWebhooksInDb()).containsOnly(new Row(webhook));
    assertNoMoreWebhookProperties();
  }

  @Test
  @UseDataProvider("DP_execute_delete_webhooks_of_components_which_is_not_a_project")
  public void execute_delete_webhooks_of_components_which_is_not_a_project(int webhookCount, String scope, String qualifier) throws SQLException {
    Project project = insertComponent(scope, qualifier, ENABLED);
    insertProjectWebhookProperties(project, webhookCount);
    IntStream.range(1, webhookCount + 1)
      .forEach(i -> insertProjectWebhookProperty(project, i, "name_" + i, "url_" + i));

    underTest.execute();

    assertThat(selectWebhooksInDb()).isEmpty();
    assertNoMoreWebhookProperties();
  }

  @DataProvider
  public static Object[][] DP_execute_delete_webhooks_of_components_which_is_not_a_project() {
    String[] scopes = {Scopes.DIRECTORY, Scopes.FILE};
    String[] qualifiers = {Qualifiers.VIEW, Qualifiers.SUBVIEW, Qualifiers.MODULE, Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE};
    int[] webhookCounts = { 1, 2, 2 + new Random().nextInt(5)};
    Object[][] res = new Object[scopes.length * qualifiers.length * webhookCounts.length][3];
    int i = 0;
    for (int webhookCount : webhookCounts) {
      for (String scope : scopes) {
        for (String qualifier : qualifiers) {
          res[i][0] = webhookCount;
          res[i][1] = scope;
          res[i][2] = qualifier;
          i++;
        }
      }
    }
    return res;
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

    assertNoMoreWebhookProperties();
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

    assertNoMoreWebhookProperties();
    assertThat(dbTester.countRowsOfTable("webhooks")).isEqualTo(1);
  }

  private void insertProperty(String key, @Nullable String value, @Nullable String resourceId, Long date) {
    dbTester.executeInsert("PROPERTIES",
      "id", randomNumeric(7),
      "prop_key", valueOf(key),
      "text_value", value,
      "is_empty", value == null || value.isEmpty(),
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

  private static long PROJECT_ID_GENERATOR = new Random().nextInt(343_343);

  private Project insertProject(boolean enabled) {
    return insertComponent(Scopes.PROJECT, Qualifiers.PROJECT, enabled);
  }

  private Project insertComponent(String scope, String qualifier, boolean enabled) {
    long projectId = PROJECT_ID_GENERATOR++;
    Project res = new Project(projectId, "prj_" + projectId);
    dbTester.executeInsert(
      "PROJECTS",
      "ID", res.id,
      "ORGANIZATION_UUID", randomAlphanumeric(15),
      "UUID", res.uuid,
      "ROOT_UUID", res.uuid,
      "PROJECT_UUID", res.uuid,
      "UUID_PATH", "." + res.uuid + ".",
      "PRIVATE", new Random().nextBoolean(),
      "SCOPE", scope,
      "QUALIFIER", qualifier,
      "ENABLED", enabled
    );
    return res;
  }

  private void insertGlobalWebhookProperties(int total) {
    insertProperty("sonar.webhooks.global",
      IntStream.range(0, total).map(i -> i + 1).mapToObj(String::valueOf).collect(Collectors.joining(",")),
      null,
      NOW);
  }

  private Webhook insertGlobalWebhookProperty(int i, @Nullable String name, @Nullable String url, String organizationUuid) {
    long createdAt = NOW + new Random().nextInt(5_6532_999);
    Webhook res = new Webhook(name, url, organizationUuid, null, createdAt);
    if (name != null) {
      insertProperty("sonar.webhooks.global." + i + ".name", name, null, createdAt);
    }
    if (url != null) {
      insertProperty("sonar.webhooks.global." + i + ".url", url, null, createdAt);
    }
    return res;
  }

  private void insertProjectWebhookProperties(Project project, int total) {
    insertProperty("sonar.webhooks.project",
      IntStream.range(0, total).map(i -> i + 1).mapToObj(String::valueOf).collect(Collectors.joining(",")),
      valueOf(project.id),
      NOW);
  }

  private Webhook insertProjectWebhookProperty(Project project, int i, @Nullable String name, @Nullable String url) {
    long createdAt = NOW + new Random().nextInt(5_6532_999);
    Webhook res = new Webhook(name, url, null, project.uuid, createdAt);
    if (name != null) {
      insertProperty("sonar.webhooks.project." + i + ".name", name, valueOf(project.id), createdAt);
    }
    if (url != null) {
      insertProperty("sonar.webhooks.project." + i + ".url", url, valueOf(project.id), createdAt);
    }
    return res;
  }

  private Stream<Row> selectWebhooksInDb() {
    return dbTester.select("select * from webhooks").stream().map(Row::new);
  }

  private void assertNoMoreWebhookProperties() {
    assertThat(dbTester.countSql("select count(*) from properties where prop_key like 'sonar.webhooks.%'"))
      .isEqualTo(0);
  }

  private static final class Webhook {
    @Nullable
    private final String name;
    @Nullable
    private final String url;
    @Nullable
    private final String organizationUuid;
    @Nullable
    private final String projectUuid;
    private final long createdAt;

    private Webhook(@Nullable String name, @Nullable String url, @Nullable String organizationUuid, @Nullable String projectUuid, long createdAt) {
      this.name = name;
      this.url = url;
      this.organizationUuid = organizationUuid;
      this.projectUuid = projectUuid;
      this.createdAt = createdAt;
    }
  }

  private static class Row {
    private final String uuid;
    private final String name;
    private final String url;
    @Nullable
    private final String organizationUuid;
    @Nullable
    private final String projectUuid;
    private final long createdAt;
    private final long updatedAt;

    private Row(Map<String, Object> row) {
      this.uuid = (String) row.get("UUID");
      this.name = (String) row.get("NAME");
      this.url = (String) row.get("URL");
      this.organizationUuid = (String) row.get("ORGANIZATION_UUID");
      this.projectUuid = (String) row.get("PROJECT_UUID");
      this.createdAt = (Long) row.get("CREATED_AT");
      this.updatedAt = (Long) row.get("UPDATED_AT");
    }

    private Row(Webhook webhook) {
      this.uuid = "NOT KNOWN YET";
      this.name = webhook.name;
      this.url = webhook.url;
      this.organizationUuid = webhook.organizationUuid;
      this.projectUuid = webhook.projectUuid;
      this.createdAt = webhook.createdAt;
      this.updatedAt = webhook.createdAt;
    }

    public String getUuid() {
      return uuid;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Row row = (Row) o;
      return createdAt == row.createdAt &&
        updatedAt == row.updatedAt &&
        Objects.equals(name, row.name) &&
        Objects.equals(url, row.url) &&
        Objects.equals(organizationUuid, row.organizationUuid) &&
        Objects.equals(projectUuid, row.projectUuid);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, url, organizationUuid, projectUuid, createdAt, updatedAt);
    }

    @Override
    public String toString() {
      return "Row{" +
        "uuid='" + uuid + '\'' +
        ", name='" + name + '\'' +
        ", url='" + url + '\'' +
        ", organizationUuid='" + organizationUuid + '\'' +
        ", projectUuid='" + projectUuid + '\'' +
        ", createdAt=" + createdAt +
        ", updatedAt=" + updatedAt +
        '}';
    }
  }

  private static final class Project {
    private final long id;
    private final String uuid;

    private Project(long id, String uuid) {
      this.id = id;
      this.uuid = uuid;
    }
  }
}
