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

import com.google.common.collect.ImmutableSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.SqlStatement;
import org.sonar.server.platform.db.migration.step.Upsert;

public class MigrateGithubAlmSettings extends DataChange {

  private static final String PROVIDER = "sonar.pullrequest.provider";

  // Global settings
  private static final String GITHUB_ENDPOINT = "sonar.pullrequest.github.endpoint";
  private static final String GITHUB_APP_ID = "sonar.alm.github.app.id";
  private static final String GITHUB_APP_PRIVATE_KEY = "sonar.alm.github.app.privateKeyContent.secured";
  private static final String GITHUB_APP_NAME = "sonar.alm.github.app.name";
  // Project setting
  private static final String GITHUB_REPOSITORY = "sonar.pullrequest.github.repository";

  private static final Set<String> MANDATORY_GLOBAL_KEYS = ImmutableSet.of(GITHUB_ENDPOINT, GITHUB_APP_ID, GITHUB_APP_PRIVATE_KEY);

  private static final String PROVIDER_VALUE = "GitHub";

  private static final String ALM_SETTING_GITHUB_ID = "github";

  private static final String INSERT_SQL = "insert into project_alm_settings (uuid, project_uuid, alm_setting_uuid, alm_repo, created_at, updated_at) values (?, ?, ?, ?, ?, ?)";
  private static final String DELETE_SQL = "delete from properties where prop_key = ? and resource_id = ?";

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public MigrateGithubAlmSettings(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Map<String, Property> globalPropertiesByKey = loadGlobalProperties(context);

    if (globalPropertiesByKey.keySet().containsAll(MANDATORY_GLOBAL_KEYS)) {
      String gitHubAlmSettingUuid = loadOrInsertAlmSetting(context, globalPropertiesByKey);
      Property globalProviderProperty = globalPropertiesByKey.getOrDefault(PROVIDER, null);
      String globalProvider = globalProviderProperty != null ? globalProviderProperty.getValue() : null;

      insertProjectAlmSettings(context, globalProvider, gitHubAlmSettingUuid);
    }

    context.prepareUpsert("delete from properties where prop_key in (?, ?, ?, ?, ?)")
      .setString(1, GITHUB_ENDPOINT)
      .setString(2, GITHUB_APP_ID)
      .setString(3, GITHUB_APP_PRIVATE_KEY)
      .setString(4, GITHUB_APP_NAME)
      .setString(5, GITHUB_REPOSITORY)
      .execute()
      .commit();
  }

  private static Map<String, Property> loadGlobalProperties(Context context) throws SQLException {
    return context
      .prepareSelect("select prop_key, text_value from properties where prop_key in (?, ?, ?, ?, ?) " +
        "and resource_id is null " +
        "and text_value is not null ")
      .setString(1, PROVIDER)
      .setString(2, GITHUB_ENDPOINT)
      .setString(3, GITHUB_APP_ID)
      .setString(4, GITHUB_APP_PRIVATE_KEY)
      .setString(5, GITHUB_APP_NAME)
      .list(Property::new)
      .stream()
      .collect(Collectors.toMap(Property::getKey, Function.identity()));
  }

  private String loadOrInsertAlmSetting(Context context, Map<String, Property> globalPropertiesByKey) throws SQLException {
    String gitHubAlmSettingUuid = loadAlmSetting(context);
    if (gitHubAlmSettingUuid != null) {
      return gitHubAlmSettingUuid;
    }
    return insertAlmSetting(context, globalPropertiesByKey);
  }

  @CheckForNull
  private static String loadAlmSetting(Context context) throws SQLException {
    List<String> list = context.prepareSelect("select uuid from alm_settings where alm_id=?")
      .setString(1, ALM_SETTING_GITHUB_ID)
      .list(row -> row.getString(1));
    if (list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }

  private String insertAlmSetting(Context context, Map<String, Property> globalPropertiesByKey) throws SQLException {
    String gitHubAlmSettingUuid = uuidFactory.create();
    Property appName = globalPropertiesByKey.get(GITHUB_APP_NAME);
    context.prepareUpsert("insert into alm_settings (uuid, alm_id, kee, url, app_id, private_key, updated_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)")
      .setString(1, gitHubAlmSettingUuid)
      .setString(2, ALM_SETTING_GITHUB_ID)
      .setString(3, appName == null ? PROVIDER_VALUE : appName.getValue())
      .setString(4, globalPropertiesByKey.get(GITHUB_ENDPOINT).getValue())
      .setString(5, globalPropertiesByKey.get(GITHUB_APP_ID).getValue())
      .setString(6, globalPropertiesByKey.get(GITHUB_APP_PRIVATE_KEY).getValue())
      .setLong(7, system2.now())
      .setLong(8, system2.now())
      .execute()
      .commit();
    return gitHubAlmSettingUuid;
  }

  private void insertProjectAlmSettings(Context context, @Nullable String globalProvider, String almSettingUuid) throws SQLException {
    final Buffer buffer = new Buffer(globalProvider);
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select prop_key, text_value, prj.uuid, prj.id from properties prop " +
      "inner join projects prj on prj.id=prop.resource_id " +
      "where prop.prop_key in (?, ?) " +
      "order by prj.id asc")
      .setString(1, PROVIDER)
      .setString(2, GITHUB_REPOSITORY);
    massUpdate.update(INSERT_SQL);
    massUpdate.update(DELETE_SQL);
    massUpdate.execute((row, update, updateIndex) -> {
      boolean shouldExecuteUpdate = false;
      String projectUuid = row.getString(3);
      Long projectId = row.getLong(4);
      // Set last projectUuid the first time
      if (buffer.getLastProjectUuid() == null) {
        buffer.setLastProject(projectUuid, projectId);
      }
      // When current projectUuid is different from last processed projectUuid, feed the prepared statement
      if (!projectUuid.equals(buffer.getLastProjectUuid())) {
        if (updateIndex == 0) {
          // Insert new row in PROJECT_ALM_SETTINGS
          shouldExecuteUpdate = updateStatementIfNeeded(buffer.getLastProjectUuid(), buffer, update, almSettingUuid);
        } else {
          // Delete old property
          shouldExecuteUpdate = deleteProperty(buffer.getLastProjectId(), buffer, update);
          buffer.clear();
          // Update last projectUuid in buffer only when it has changed
          buffer.setLastProject(projectUuid, projectId);
        }
      }
      // Update remaining buffer values only once, and only after delete
      if (updateIndex == 1) {
        String propertyKey = row.getString(1);
        String propertyValue = row.getString(2);
        if (propertyKey.equals(PROVIDER)) {
          buffer.setProvider(propertyValue);
        } else if (propertyKey.equals(GITHUB_REPOSITORY)) {
          buffer.setRepository(propertyValue);
        }
        buffer.setCurrentProject(projectUuid, projectId);
      }
      return shouldExecuteUpdate;
    });
    String projectUuid = buffer.getCurrentProjectUuid();
    if (projectUuid == null) {
      return;
    }
    // Process last entry
    Upsert upsert = context.prepareUpsert(INSERT_SQL);
    if (updateStatementIfNeeded(projectUuid, buffer, upsert, almSettingUuid)) {
      upsert.execute().commit();
    }
    if (buffer.shouldDelete()) {
      context.prepareUpsert(DELETE_SQL)
        .setString(1, GITHUB_REPOSITORY)
        .setLong(2, buffer.getCurrentProjectId())
        .execute()
        .commit();
    }
  }

  private static boolean deleteProperty(long effectiveProjectId, Buffer buffer, SqlStatement update) throws SQLException {
    if (buffer.shouldDelete()) {
      update.setString(1, GITHUB_REPOSITORY);
      update.setLong(2, effectiveProjectId);
      return true;
    }
    return false;
  }

  private boolean updateStatementIfNeeded(String effectiveProjectUuid, Buffer buffer, SqlStatement update, String almSettingUuid)
    throws SQLException {
    if (!buffer.shouldUpdate()) {
      return false;
    }
    update.setString(1, uuidFactory.create());
    update.setString(2, effectiveProjectUuid);
    update.setString(3, almSettingUuid);
    update.setString(4, buffer.getRepository());
    update.setLong(5, system2.now());
    update.setLong(6, system2.now());
    return true;
  }

  private static class Property {
    private final String key;
    private final String value;

    Property(Select.Row row) throws SQLException {
      this.key = row.getString(1);
      this.value = row.getString(2);
    }

    String getKey() {
      return key;
    }

    String getValue() {
      return value;
    }
  }

  private static class Buffer {
    private final String globalProvider;
    private String lastProjectUuid;
    private String currentProjectUuid;
    private Long lastProjectId;
    private Long currentProjectId;
    private String provider;
    private String repository;

    public Buffer(@Nullable String globalProvider) {
      this.globalProvider = globalProvider;
    }

    Buffer setLastProject(@Nullable String projectUuid, @Nullable Long projectId) {
      this.lastProjectUuid = projectUuid;
      this.lastProjectId = projectId;
      return this;
    }

    @CheckForNull
    String getLastProjectUuid() {
      return lastProjectUuid;
    }

    @CheckForNull
    Long getLastProjectId() {
      return lastProjectId;
    }

    Buffer setCurrentProject(@Nullable String projectUuid, @Nullable Long projectId) {
      this.currentProjectUuid = projectUuid;
      this.currentProjectId = projectId;
      return this;
    }

    @CheckForNull
    String getCurrentProjectUuid() {
      return currentProjectUuid;
    }

    @CheckForNull
    Long getCurrentProjectId() {
      return currentProjectId;
    }

    Buffer setProvider(@Nullable String provider) {
      this.provider = provider;
      return this;
    }

    @CheckForNull
    String getRepository() {
      return repository;
    }

    Buffer setRepository(@Nullable String repository) {
      this.repository = repository;
      return this;
    }

    boolean shouldUpdate() {
      if (repository == null) {
        return false;
      }
      if (Objects.equals(provider, PROVIDER_VALUE)) {
        return true;
      }
      return provider == null && Objects.equals(globalProvider, PROVIDER_VALUE);
    }

    boolean shouldDelete() {
      if (provider != null) {
        return provider.equals(PROVIDER_VALUE);
      }
      return Objects.equals(globalProvider, PROVIDER_VALUE);
    }

    void clear() {
      this.lastProjectUuid = null;
      this.currentProjectUuid = null;
      this.lastProjectId = null;
      this.currentProjectId = null;
      this.provider = null;
      this.repository = null;
    }
  }

}
