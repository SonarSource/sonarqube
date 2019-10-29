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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
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

public class MigrateAzureAlmSettings extends DataChange {

  private static final String PROVIDER = "sonar.pullrequest.provider";
  private static final String AZURE_TOKEN = "sonar.pullrequest.vsts.token.secured";
  private static final String PROVIDER_VALUE = "Azure DevOps";
  private static final String ALM_SETTING_ID = "azure_devops";

  private static final String INSERT_SQL = "insert into project_alm_settings (uuid, project_uuid, alm_setting_uuid, created_at, updated_at) values (?, ?, ?, ?, ?)";
  private static final String DELETE_SQL = "delete from properties where prop_key = ? and resource_id=?";

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public MigrateAzureAlmSettings(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    List<String> tokens = loadTokens(context);
    if (tokens.isEmpty()) {
      return;
    }
    Map<String, Property> globalPropertiesByKey = loadGlobalProperties(context);
    Map<String, String> almSettingUuidByTokens = loadOrInsertAlmSettings(context, tokens);

    String globalProvider = getGlobalProperty(globalPropertiesByKey, PROVIDER);
    String globalToken = getGlobalProperty(globalPropertiesByKey, AZURE_TOKEN);

    insertProjectAlmSettings(context, globalProvider, globalToken, almSettingUuidByTokens);
    context.prepareUpsert("delete from properties where prop_key = ?")
      .setString(1, AZURE_TOKEN)
      .execute()
      .commit();
  }

  @CheckForNull
  private static String getGlobalProperty(Map<String, Property> globalPropertiesByKey, String propertyKey) {
    Property globalProperty = globalPropertiesByKey.getOrDefault(propertyKey, null);
    return globalProperty != null ? globalProperty.getValue() : null;
  }

  private static Map<String, Property> loadGlobalProperties(Context context) throws SQLException {
    return context
      .prepareSelect("select prop_key, text_value from properties where prop_key in (?, ?) " +
        "and resource_id is null " +
        "and text_value is not null ")
      .setString(1, PROVIDER)
      .setString(2, AZURE_TOKEN)
      .list(Property::new)
      .stream()
      .collect(Collectors.toMap(Property::getKey, Function.identity()));
  }

  private static List<String> loadTokens(Context context) throws SQLException {
    return new ArrayList<>(context
      .prepareSelect("select distinct text_value from properties where prop_key = ?")
      .setString(1, AZURE_TOKEN)
      .list(row -> row.getString(1)));
  }

  private Map<String, String> loadOrInsertAlmSettings(Context context, List<String> tokens) throws SQLException {
    Map<String, String> almSettingUuidByTokens = loadExistingAlmSettingUuidByToken(context);
    TreeSet<String> tokensWithNoAlmSetting = new TreeSet<>(tokens);
    tokensWithNoAlmSetting.removeAll(almSettingUuidByTokens.keySet());
    if (tokensWithNoAlmSetting.isEmpty()) {
      return almSettingUuidByTokens;
    }
    int index = almSettingUuidByTokens.isEmpty() ? 0 : (almSettingUuidByTokens.size() + 1);
    if (tokensWithNoAlmSetting.size() == 1) {
      String token = tokensWithNoAlmSetting.first();
      String almSettingUuid = insertAlmSetting(context, token, index);
      almSettingUuidByTokens.put(token, almSettingUuid);
      return almSettingUuidByTokens;
    }
    for (String token : tokensWithNoAlmSetting) {
      String almSettingUuid = insertAlmSetting(context, token, index);
      almSettingUuidByTokens.put(token, almSettingUuid);
      index++;
    }
    return almSettingUuidByTokens;
  }

  private static Map<String, String> loadExistingAlmSettingUuidByToken(Context context) throws SQLException {
    Select select = context.prepareSelect("select uuid, pat from alm_settings where alm_id=?")
      .setString(1, ALM_SETTING_ID);
    return select
      .list(row -> new AlmSetting(row.getString(1), row.getString(2)))
      .stream()
      .collect(Collectors.toMap(AlmSetting::getPersonalAccessToken, AlmSetting::getUuid));
  }

  private String insertAlmSetting(Context context, String token, int index) throws SQLException {
    String almSettingUuid = uuidFactory.create();
    context.prepareUpsert("insert into alm_settings (uuid, alm_id, kee, pat, updated_at, created_at) values (?, ?, ?, ?, ?, ?)")
      .setString(1, almSettingUuid)
      .setString(2, ALM_SETTING_ID)
      .setString(3, index == 0 ? PROVIDER_VALUE : (PROVIDER_VALUE + " " + index))
      .setString(4, token)
      .setLong(5, system2.now())
      .setLong(6, system2.now())
      .execute()
      .commit();
    return almSettingUuid;
  }

  private void insertProjectAlmSettings(Context context, @Nullable String globalProvider, @Nullable String globalToken,
    Map<String, String> almSettingUuidByTokens) throws SQLException {
    final Buffer buffer = new Buffer(globalProvider);
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select prop_key, text_value, prj.uuid, prj.id from properties prop " +
      "inner join projects prj on prj.id=prop.resource_id " +
      "where prop.prop_key in (?, ?) " +
      "order by prj.id asc")
      .setString(1, PROVIDER)
      .setString(2, AZURE_TOKEN);
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
          shouldExecuteUpdate = updateStatementIfNeeded(buffer, buffer.getLastProjectUuid(), update, globalToken, almSettingUuidByTokens);
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
        } else if (propertyKey.equals(AZURE_TOKEN)) {
          buffer.setToken(propertyValue);
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
    boolean shouldExecuteInsert = updateStatementIfNeeded(buffer, projectUuid, upsert, globalToken, almSettingUuidByTokens);
    if (shouldExecuteInsert) {
      upsert.execute().commit();
    }
    if (buffer.shouldUpdate()) {
      context.prepareUpsert(DELETE_SQL)
        .setString(1, AZURE_TOKEN)
        .setLong(2, buffer.getCurrentProjectId())
        .execute()
        .commit();
    }
  }

  private static boolean deleteProperty(long effectiveProjectId, Buffer buffer, SqlStatement update) throws SQLException {
    if (buffer.shouldUpdate()) {
      update.setString(1, AZURE_TOKEN);
      update.setLong(2, effectiveProjectId);
      return true;
    }
    return false;
  }

  private boolean updateStatementIfNeeded(Buffer buffer, String projectUuid, SqlStatement update, @Nullable String globalToken,
    Map<String, String> almSettingUuidByTokens)
    throws SQLException {
    String token = buffer.getToken();
    String almSettingUuid = token == null ? almSettingUuidByTokens.get(globalToken) : almSettingUuidByTokens.get(token);
    if (!buffer.shouldUpdate() || almSettingUuid == null) {
      return false;
    }
    update.setString(1, uuidFactory.create());
    update.setString(2, projectUuid);
    update.setString(3, almSettingUuid);
    update.setLong(4, system2.now());
    update.setLong(5, system2.now());
    return true;
  }

  private static class AlmSetting {
    private final String uuid;
    private final String personalAccessToken;

    AlmSetting(String uuid, String personalAccessToken) {
      this.uuid = uuid;
      this.personalAccessToken = personalAccessToken;
    }

    String getUuid() {
      return uuid;
    }

    String getPersonalAccessToken() {
      return personalAccessToken;
    }
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
    private String token;

    private Buffer(@Nullable String globalProvider) {
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
    String getToken() {
      return token;
    }

    Buffer setToken(@Nullable String token) {
      this.token = token;
      return this;
    }

    boolean shouldUpdate() {
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
      this.token = null;
    }
  }

}
