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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;
import org.sonar.server.platform.db.migration.version.v63.DefaultOrganizationUuidProvider;

import static java.util.Collections.emptyList;
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
      .prepareSelect("select id, prop_key, resource_id, text_value, created_at from properties")
      .list(row -> new PropertyRow(
        row.getLong(1),
        row.getString(2),
        row.getLong(3),
        row.getString(4),
        row.getLong(5)))
      .stream()
      .filter(row -> row.key().startsWith("sonar.webhooks"))
      .collect(toMap(PropertyRow::key, Function.identity()));

    if (!rows.isEmpty()) {
      final List<Long> toBeDeleted = new ArrayList<>();
      toBeDeleted.addAll(migrateGlobalWebhooks(context, rows));
      toBeDeleted.addAll(migrateProjectsWebhooks(context, rows, getProjectsUuids(context)));
      deleteFromProperties(context, toBeDeleted);
    }

  }

  private static Map<Long, String> getProjectsUuids(Context context) throws SQLException {
    return context
      .prepareSelect("select id, uuid from projects")
      .list(row -> new ProjectFlyweight(
        row.getLong(1),
        row.getString(2)))
      .stream()
      .collect(toMap(ProjectFlyweight::id, ProjectFlyweight::uuid));
  }

  private List<Long> migrateProjectsWebhooks(Context context, Map<String, PropertyRow> properties, Map<Long, String> projectsKeys) throws SQLException {
    List<Long> toBeDeleted = new ArrayList<>();
    PropertyRow index = properties.get("sonar.webhooks.project");
    if (index == null) {
      return emptyList();
    }

    // can't lambda due to checked exception.
    for (Webhook webhook : extractProjectWebhooksFrom(properties, index.value().split(","), projectsKeys)) {
      toBeDeleted.addAll(insert(context, webhook));
    }

    toBeDeleted.add(index.id());
    return toBeDeleted;
  }

  private List<Long> migrateGlobalWebhooks(Context context, Map<String, PropertyRow> properties) throws SQLException {

    List<Long> toBeDeleted = new ArrayList<>();
    PropertyRow index = properties.get("sonar.webhooks.global");
    if (index == null) {
      return emptyList();
    }

    // can't lambda due to checked exception.
    for (Webhook webhook : extractGlobalWebhooksFrom(context, properties, index.value().split(","))) {
      toBeDeleted.addAll(insert(context, webhook));
    }

    toBeDeleted.add(index.id());
    return toBeDeleted;
  }

  private List<Long> insert(Context context, Webhook webhook) throws SQLException {

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

    return webhook.rowIds();
  }

  private static void deleteFromProperties(Context context, List<Long> toBeDeleted) throws SQLException {
    Upsert upsert = context.prepareUpsert(prepareDeleteRequest(toBeDeleted));
    int index = 0;
    for (Long id : toBeDeleted) {
      index++;
      upsert.setLong(index, id);
    }
    upsert.execute().commit();
  }

  private static String prepareDeleteRequest(List<Long> toBeDeleted) {
    StringBuilder sb = new StringBuilder()
      .append("delete from properties where id in (");
    toBeDeleted.forEach(n ->
      sb.append("?, ")
    );
    sb.setLength(sb.length() - 2);
    sb.append(")");
    return sb.toString();
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

  private static List<Webhook> extractProjectWebhooksFrom(Map<String, PropertyRow> properties, String[] values, Map<Long, String> projectsKeys) {
    return Arrays.stream(values)
      .map(value -> new Webhook(
        properties.get("sonar.webhooks.project." + value + ".name"),
        properties.get("sonar.webhooks.project." + value + ".url"),
        null, projectsKeys))
      .collect(toList());
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

    public Webhook(@Nullable PropertyRow name, @Nullable PropertyRow url, @Nullable String organisationUuid, @Nullable Map<Long, String> projectsKeys) {
      this.name = name;
      this.url = url;
      this.organisationUuid = organisationUuid;
      if (StringUtils.isBlank(organisationUuid) && name != null && projectsKeys != null) {
        this.projectUuid = projectsKeys.get(name.resourceId());
      }
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

    public List<Long> rowIds() {
      List<Long> toBeDropped = new ArrayList<>();
      if (name != null) {
        toBeDropped.add(name.id());
      }
      if (url != null) {
        toBeDropped.add(url.id());
      }
      return toBeDropped;
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

  private static class ProjectFlyweight {

    private final Long id;
    private final String uuid;

    public ProjectFlyweight(Long id, String uuid) {
      this.id = id;
      this.uuid = uuid;
    }

    public Long id() {
      return id;
    }

    public String uuid() {
      return uuid;
    }
  }

}
