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

import com.google.common.collect.Multimap;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

import static java.util.stream.Collectors.toList;

public class MigrateWebhooksToWebhooksTable extends DataChange {

  private static final long NO_RESOURCE_ID = -8_435_121;
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
    Multimap<Long, PropertyRow> rows = context
      .prepareSelect("select" +
        " props.id, props.prop_key, props.resource_id, prj.uuid, props.text_value, props.created_at" +
        " from properties props" +
        " left join projects prj on prj.id = props.resource_id and prj.scope = ? and prj.qualifier = ? and prj.enabled = ?" +
        " where" +
        " props.prop_key like 'sonar.webhooks%'" +
        " and props.text_value is not null")
      .setString(1, "PRJ")
      .setString(2, "TRK")
      .setBoolean(3, true)
      .list(row -> new PropertyRow(
        row.getLong(1),
        row.getString(2),
        row.getNullableLong(3),
        row.getNullableString(4),
        row.getString(5),
        row.getLong(6)))
      .stream()
      .collect(MoreCollectors.index(PropertyRow::getResourceId, Function.identity()));

    for (Map.Entry<Long, Collection<PropertyRow>> entry : rows.asMap().entrySet()) {
      long projectId = entry.getKey();
      if (projectId == NO_RESOURCE_ID) {
        migrateGlobalWebhooks(context, entry.getValue());
      } else {
        migrateProjectsWebhooks(context, entry.getValue());
      }
      deleteAllWebhookProperties(context);
    }
  }

  private static void deleteAllWebhookProperties(Context context) throws SQLException {
    context
      .prepareUpsert("delete from properties where prop_key like 'sonar.webhooks.global%' or prop_key like 'sonar.webhooks.project%'")
      .execute()
      .commit();
  }

  private void migrateGlobalWebhooks(Context context, Collection<PropertyRow> rows) throws SQLException {
    Multimap<String, PropertyRow> rowsByPropertyKey = rows.stream()
      .collect(MoreCollectors.index(PropertyRow::getPropertyKey));
    Optional<PropertyRow> rootProperty = rowsByPropertyKey.get("sonar.webhooks.global").stream().findFirst();
    if (rootProperty.isPresent()) {
      PropertyRow row = rootProperty.get();
      // can't lambda due to checked exception.
      for (Webhook webhook : extractGlobalWebhooksFrom(context, rowsByPropertyKey, row.value().split(","))) {
        insert(context, webhook);
      }
    }
  }

  private List<Webhook> extractGlobalWebhooksFrom(Context context, Multimap<String, PropertyRow> rowsByPropertyKey, String[] values) throws SQLException {
    String defaultOrganizationUuid = defaultOrganizationUuidProvider.get(context);
    return Arrays.stream(values)
      .map(value -> {
        Optional<PropertyRow> name = rowsByPropertyKey.get("sonar.webhooks.global." + value + ".name").stream().findFirst();
        Optional<PropertyRow> url = rowsByPropertyKey.get("sonar.webhooks.global." + value + ".url").stream().findFirst();
        if (name.isPresent() && url.isPresent()) {
          return new Webhook(
            name.get(),
            url.get(),
            defaultOrganizationUuid,
            null);
        }
        LOGGER.warn(
          "Global webhook missing name and/or url will be deleted (name='{}', url='{}')",
          name.map(PropertyRow::value).orElse(null),
          url.map(PropertyRow::value).orElse(null));
        return null;
      })
      .filter(Objects::nonNull)
      .collect(toList());
  }

  private void migrateProjectsWebhooks(Context context, Collection<PropertyRow> rows) throws SQLException {
    Multimap<String, PropertyRow> rowsByPropertyKey = rows.stream()
      .collect(MoreCollectors.index(PropertyRow::getPropertyKey));
    Optional<PropertyRow> rootProperty = rowsByPropertyKey.get("sonar.webhooks.project").stream().findFirst();
    if (rootProperty.isPresent()) {
      PropertyRow row = rootProperty.get();
      if (row.getProjectUuid() == null) {
        LOGGER.warn("At least one webhook referenced missing or non project resource '{}' and will be deleted", row.getResourceId());
      } else {
        for (Webhook webhook : extractProjectWebhooksFrom(row, rowsByPropertyKey, row.value().split(","))) {
          insert(context, webhook);
        }
      }
    }
  }

  private static List<Webhook> extractProjectWebhooksFrom(PropertyRow row, Multimap<String, PropertyRow> properties, String[] values) {
    return Arrays.stream(values)
      .map(value -> {
        Optional<PropertyRow> name = properties.get("sonar.webhooks.project." + value + ".name").stream().findFirst();
        Optional<PropertyRow> url = properties.get("sonar.webhooks.project." + value + ".url").stream().findFirst();
        if (name.isPresent() && url.isPresent()) {
          return new Webhook(name.get(), url.get(), null, row.projectUuid);
        }
        LOGGER.warn("Project webhook for project {} (id={}) missing name and/or url will be deleted (name='{}', url='{}')",
          row.getProjectUuid(), row.getResourceId(), name.map(PropertyRow::value).orElse(null), url.map(PropertyRow::value).orElse(null));
        return null;
      })
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toList());
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

  private static class PropertyRow {
    private final Long id;
    private final String propertyKey;
    private final Long resourceId;
    private final String projectUuid;
    private final String value;
    private final Long createdAt;

    private PropertyRow(long id, String propertyKey, @Nullable Long resourceId, @Nullable String projectUuid, String value, Long createdAt) {
      this.id = id;
      this.propertyKey = propertyKey;
      this.resourceId = resourceId;
      this.projectUuid = projectUuid;
      this.value = value;
      this.createdAt = createdAt;
    }

    public Long id() {
      return id;
    }

    public String getPropertyKey() {
      return propertyKey;
    }

    public long getResourceId() {
      return resourceId == null ? NO_RESOURCE_ID : resourceId;
    }

    @CheckForNull
    public String getProjectUuid() {
      return projectUuid;
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
        ", propertyKey='" + propertyKey + '\'' +
        ", resourceId=" + resourceId +
        ", projectUuid=" + projectUuid +
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
