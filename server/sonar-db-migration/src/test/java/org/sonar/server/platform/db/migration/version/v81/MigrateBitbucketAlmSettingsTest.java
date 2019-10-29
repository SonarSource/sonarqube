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

package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class MigrateBitbucketAlmSettingsTest {

  private final static long PAST = 10_000_000_000L;
  private static final long NOW = 50_000_000_000L;
  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateBitbucketAlmSettingsTest.class, "schema.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private DataChange underTest = new MigrateBitbucketAlmSettings(db.database(), uuidFactory, system2);

  @Test
  public void migrate_settings_when_global_provider_is_set_to_bitbucket() throws SQLException {
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId1 = insertProject("PROJECT_1");
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository1", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug1", projectId1);
    long projectId2 = insertProject("PROJECT_2");
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository2", projectId2);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug2", projectId2);

    underTest.execute();

    assertAlmSettings(tuple("bitbucket", "Bitbucket Server", "https://enterprise.com", "12345", NOW, NOW));
    String almSettingUuid = selectAlmSettingUuid("Bitbucket Server");
    assertProjectAlmSettings(
      tuple("PROJECT_1", almSettingUuid, "Repository1", "Slug1", NOW, NOW),
      tuple("PROJECT_2", almSettingUuid, "Repository2", "Slug2", NOW, NOW));
    assertProperties(tuple("sonar.pullrequest.provider", "Bitbucket Server", null));
  }

  @Test
  public void migrate_settings_when_project_provider_is_set_to_bitbucket_and_global_provider_is_set_to_something_else() throws SQLException {
    insertProperty("sonar.pullrequest.provider", "Azure DevOps", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId1 = insertProject("PROJECT_1");
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository1", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug1", projectId1);
    long projectId2 = insertProject("PROJECT_2");
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", projectId2);
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository2", projectId2);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug2", projectId2);

    underTest.execute();

    assertAlmSettings(tuple("bitbucket", "Bitbucket Server", "https://enterprise.com", "12345", NOW, NOW));
    String almSettingUuid = selectAlmSettingUuid("Bitbucket Server");
    assertProjectAlmSettings(
      tuple("PROJECT_1", almSettingUuid, "Repository1", "Slug1", NOW, NOW),
      tuple("PROJECT_2", almSettingUuid, "Repository2", "Slug2", NOW, NOW));
    assertProperties(
      tuple("sonar.pullrequest.provider", "Azure DevOps", null),
      tuple("sonar.pullrequest.provider", "Bitbucket Server", projectId1),
      tuple("sonar.pullrequest.provider", "Bitbucket Server", projectId2));
  }

  @Test
  public void delete_bitbucket_settings_when_project_provider_is_not_set_to_bitbucket() throws SQLException {
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId1 = insertProject("PROJECT_1");
    // Project provider is set to something else
    insertProperty("sonar.pullrequest.provider", "Azure", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository1", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug1", projectId1);

    underTest.execute();

    assertNoProjectAlmSettings();
    assertProperties(
      tuple("sonar.pullrequest.provider", "Bitbucket Server", null),
      tuple("sonar.pullrequest.provider", "Azure", projectId1));
  }

  @Test
  public void ignore_none_bitbucket_project_settings() throws SQLException {
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId1 = insertProject("PROJECT_1");
    insertProperty("sonar.pullrequest.provider", "Azure", projectId1);
    long projectId2 = insertProject("PROJECT_2");
    insertProperty("sonar.pullrequest.provider", "Azure", projectId2);

    underTest.execute();

    assertAlmSettings(tuple("bitbucket", "Bitbucket Server", "https://enterprise.com", "12345", NOW, NOW));
    assertNoProjectAlmSettings();
    assertProperties(
      tuple("sonar.pullrequest.provider", "Bitbucket Server", null),
      tuple("sonar.pullrequest.provider", "Azure", projectId1),
      tuple("sonar.pullrequest.provider", "Azure", projectId2));
  }

  @Test
  public void use_existing_alm_setting() throws SQLException {
    db.executeInsert("alm_settings",
      "uuid", "ABCD",
      "alm_id", "bitbucket",
      "kee", "Bitbucket Server",
      "url", "https://enterprise.com",
      "pat", "12345",
      "created_at", PAST,
      "updated_at", PAST);
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId1 = insertProject("PROJECT_1");
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository1", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug1", projectId1);

    underTest.execute();

    assertProjectAlmSettings(tuple("PROJECT_1", "ABCD", "Repository1", "Slug1", NOW, NOW));
    assertProperties(tuple("sonar.pullrequest.provider", "Bitbucket Server", null));
  }

  @Test
  public void do_not_create_alm_settings_when_missing_some_global_properties() throws SQLException {
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    // No token
    long projectId1 = insertProject("PROJECT_1");
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository1", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug1", projectId1);

    underTest.execute();

    assertNoAlmSettings();
    assertNoProjectAlmSettings();
    assertProperties(tuple("sonar.pullrequest.provider", "Bitbucket Server", null));
  }

  @Test
  public void do_not_create_project_alm_settings_when_missing_some_project_properties() throws SQLException {
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId1 = insertProject("PROJECT_1");
    // No project
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug1", projectId1);
    long projectId2 = insertProject("PROJECT_2");
    // No repository
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Project1", projectId2);

    underTest.execute();

    assertAlmSettings(tuple("bitbucket", "Bitbucket Server", "https://enterprise.com", "12345", NOW, NOW));
    assertNoProjectAlmSettings();
    assertProperties(tuple("sonar.pullrequest.provider", "Bitbucket Server", null));
  }

  @Test
  public void do_nothing_when_no_alm_properties() throws SQLException {
    insertProperty("sonar.other.property", "Something", null);

    underTest.execute();

    assertNoAlmSettings();
    assertNoProjectAlmSettings();
    assertProperties(tuple("sonar.other.property", "Something", null));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", null);
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId1 = insertProject("PROJECT_1");
    insertProperty("sonar.pullrequest.provider", "Bitbucket Server", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository1", projectId1);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug1", projectId1);
    underTest.execute();

    // Global settings have been removed, let's re-create them
    insertProperty("sonar.pullrequest.bitbucketserver.serverUrl", "https://enterprise.com", null);
    insertProperty("sonar.pullrequest.bitbucketserver.token.secured", "12345", null);
    long projectId2 = insertProject("PROJECT_2");
    insertProperty("sonar.pullrequest.bitbucketserver.project", "Repository2", projectId2);
    insertProperty("sonar.pullrequest.bitbucketserver.repository", "Slug2", projectId2);
    underTest.execute();

    assertAlmSettings(tuple("bitbucket", "Bitbucket Server", "https://enterprise.com", "12345", NOW, NOW));
    String almSettingUuid = selectAlmSettingUuid("Bitbucket Server");
    assertProperties(
      tuple("sonar.pullrequest.provider", "Bitbucket Server", null),
      tuple("sonar.pullrequest.provider", "Bitbucket Server", projectId1));
  }

  private void assertAlmSettings(Tuple... expectedTuples) {
    assertThat(db.select("SELECT alm_id, kee, url, pat, created_at, updated_at FROM alm_settings")
      .stream()
      .map(map -> new Tuple(map.get("ALM_ID"), map.get("KEE"), map.get("URL"), map.get("PAT"), map.get("CREATED_AT"),
        map.get("UPDATED_AT")))
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void assertNoAlmSettings() {
    assertAlmSettings();
  }

  private void assertProjectAlmSettings(Tuple... expectedTuples) {
    assertThat(db.select("SELECT project_uuid, alm_setting_uuid, alm_repo, alm_slug, created_at, updated_at FROM project_alm_settings")
      .stream()
      .map(map -> new Tuple(map.get("PROJECT_UUID"), map.get("ALM_SETTING_UUID"), map.get("ALM_REPO"), map.get("ALM_SLUG"), map.get("CREATED_AT"), map.get("UPDATED_AT")))
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void assertNoProjectAlmSettings() {
    assertProjectAlmSettings();
  }

  private void assertProperties(Tuple... expectedTuples) {
    assertThat(db.select("SELECT prop_key, text_value, resource_id FROM properties")
      .stream()
      .map(map -> new Tuple(map.get("PROP_KEY"), map.get("TEXT_VALUE"), map.get("RESOURCE_ID")))
      .collect(toSet()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void assertNoProperties() {
    assertProperties();
  }

  private String selectAlmSettingUuid(String almSettingKey) {
    return (String) db.selectFirst("select uuid from alm_settings where kee='" + almSettingKey + "'").get("UUID");
  }

  private void insertProperty(String key, String value, @Nullable Long projectId) {
    db.executeInsert(
      "PROPERTIES",
      "PROP_KEY", key,
      "RESOURCE_ID", projectId,
      "USER_ID", null,
      "IS_EMPTY", false,
      "TEXT_VALUE", value,
      "CLOB_VALUE", null,
      "CREATED_AT", System2.INSTANCE.now());
  }

  private long insertProject(String uuid) {
    int id = nextInt();
    db.executeInsert("PROJECTS",
      "ID", id,
      "ORGANIZATION_UUID", "default",
      "KEE", uuid + "-key",
      "UUID", uuid,
      "PROJECT_UUID", uuid,
      "MAIN_BRANCH_PROJECT_UUID", uuid,
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", Boolean.toString(false),
      "SCOPE", "PRJ",
      "QUALIFIER", "PRJ");
    return id;
  }

}
