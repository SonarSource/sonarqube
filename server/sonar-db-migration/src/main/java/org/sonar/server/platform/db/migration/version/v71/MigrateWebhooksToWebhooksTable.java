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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class MigrateWebhooksToWebhooksTable extends DataChange {

  private static final Logger LOGGER = Loggers.get(MigrateWebhooksToWebhooksTable.class);

  private DefaultOrganizationUuidProvider defaultOrganizationUuidProvider;
  private UuidFactory uuidFactory;

  public MigrateWebhooksToWebhooksTable(Database db, DefaultOrganizationUuidProvider defaultOrganizationUuidProvider, UuidFactory uuidFactory) {
    super(db);
    this.defaultOrganizationUuidProvider = defaultOrganizationUuidProvider;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    Map<String, PropertyRow> rows = context
      .prepareSelect("select id, prop_key, resource_id, text_value, created_at from properties where prop_key like 'sonar.webhooks%'")
      .list(row -> new PropertyRow(
        row.getLong(1),
        row.getString(2),
        row.getLong(3),
        row.getString(4),
        row.getLong(5)))
      .stream()
      .collect(toMap(PropertyRow::key, Function.identity()));

    if (!rows.isEmpty()) {
      migrateGlobalWebhooks(context, rows);
      migrateProjectsWebhooks(context, rows);
      context
        .prepareUpsert("delete from properties where prop_key like 'sonar.webhooks.global%' or prop_key like 'sonar.webhooks.project%'")
        .execute()
        .commit();
    }
  }

  private void migrateProjectsWebhooks(Context context, Map<String, PropertyRow> properties) throws SQLException {
    PropertyRow index = properties.get("sonar.webhooks.project");
    if (index != null) {
      // can't lambda due to checked exception.
      for (Webhook webhook : extractProjectWebhooksFrom(context, properties, index.value().split(","))) {
        insert(context, webhook);
      }
    }
  }

  private void migrateGlobalWebhooks(Context context, Map<String, PropertyRow> properties) throws SQLException {
    PropertyRow index = properties.get("sonar.webhooks.global");
    if (index != null) {
      // can't lambda due to checked exception.
      for (Webhook webhook : extractGlobalWebhooksFrom(context, properties, index.value().split(","))) {
        insert(context, webhook);
      }
    }
  }

  private void insert(Context context, Webhook webhook) throws SQLException {
    if (webhook.isValid()) {
      context.prepareUpsert("insert into webhooks (uuid, name, url, organization_uuid, project_uuid, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)")
        .setString(1, uuidFactory.create())
        .setString(2, webhook.name())
        .setString(3, webhook.url())
        .setString(4, webhook.organisationUuid())
        .setString(5, webhook.projectUuid())
        .setLong(6, webhook.createdAt())
        .setLong(7, webhook.createdAt())
        .execute()
        .commit();
    } else {
      LOGGER.info("Unable to migrate inconsistent webhook (entry deleted from PROPERTIES) : " + webhook);
    }
  }

  private List<Webhook> extractGlobalWebhooksFrom(Context context, Map<String, PropertyRow> properties, String[] values) throws SQLException {
    String defaultOrganizationUuid = defaultOrganizationUuidProvider.get(context);
    return Arrays.stream(values)
      .map(value -> new Webhook(
        properties.get("sonar.webhooks.global." + value + ".name"),
        properties.get("sonar.webhooks.global." + value + ".url"),
        defaultOrganizationUuid, null))
      .collect(toList());
  }

  private static List<Webhook> extractProjectWebhooksFrom(Context context, Map<String, PropertyRow> properties, String[] values) throws SQLException {
    List<Webhook> webhooks = new ArrayList<>();
    for (String value : values) {
      PropertyRow name = properties.get("sonar.webhooks.project." + value + ".name");
      PropertyRow url = properties.get("sonar.webhooks.project." + value + ".url");
      webhooks.add(new Webhook(name, url, null, projectUuidOf(context, name)));
    }
    return webhooks;
  }

  private static String projectUuidOf(Context context, PropertyRow row) throws SQLException {
    return context
      .prepareSelect("select uuid from projects where id = ?")
      .setLong(1, row.resourceId())
      .list(row1 -> row1.getString(1)).stream().findFirst().orElse(null);
  }

  private static class PropertyRow {

    private final Long id;
    private final String key;
    private final Long resourceId;
    private final String value;
    private final Long createdAt;

    public PropertyRow(long id, String key, Long resourceId, String value, Long createdAt) {
      this.id = id;
      this.key = key;
      this.resourceId = resourceId;
      this.value = value;
      this.createdAt = createdAt;
    }

    public Long id() {
      return id;
    }

    public String key() {
      return key;
    }

    public Long resourceId() {
      return resourceId;
    }

    public String value() {
      return value;
    }

    public Long createdAt() {
      return createdAt;
    }

    @Override
    public String toString() {
      return "{" +
        "id=" + id +
        ", key='" + key + '\'' +
        ", resourceId=" + resourceId +
        ", value='" + value + '\'' +
        ", createdAt=" + createdAt +
        '}';
    }
  }

  private static class Webhook {

    private final PropertyRow name;
    private final PropertyRow url;
    private String organisationUuid;
    private String projectUuid;

    public Webhook(@Nullable PropertyRow name, @Nullable PropertyRow url, @Nullable String organisationUuid, @Nullable String projectUuid) {
      this.name = name;
      this.url = url;
      this.organisationUuid = organisationUuid;
      this.projectUuid = projectUuid;
    }

    public String name() {
      return name.value();
    }

    public String url() {
      return url.value();
    }

    public String organisationUuid() {
      return organisationUuid;
    }

    public String projectUuid() {
      return projectUuid;
    }

    public Long createdAt() {
      return name.createdAt();
    }

    public boolean isValid() {
      return name != null && url != null && name() != null && url() != null && (organisationUuid() != null || projectUuid() != null) && createdAt() != null;
    }

    @Override
    public String toString() {
      final StringBuilder s = new StringBuilder().append("Webhook{").append("name=").append(name);
      if (name != null) {
        s.append(name.toString());
      }
      s.append(", url=").append(url);
      if (url != null) {
        s.append(url.toString());
      }
      s.append(", organisationUuid='").append(organisationUuid).append('\'')
        .append(", projectUuid='").append(projectUuid).append('\'').append('}');
      return s.toString();
    }
  }

}
