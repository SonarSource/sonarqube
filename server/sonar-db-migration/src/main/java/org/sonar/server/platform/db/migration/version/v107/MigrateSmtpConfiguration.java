/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v107;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Select;
import org.sonar.server.platform.db.migration.step.Upsert;

import static java.util.stream.Collectors.joining;

public class MigrateSmtpConfiguration extends DataChange {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateSmtpConfiguration.class.getName());

  @VisibleForTesting
  static final String EMAIL_SMTP_HOST_SECURED = "email.smtp_host.secured";
  static final String EMAIL_SMTP_PORT_SECURED = "email.smtp_port.secured";
  static final String EMAIL_SMTP_SECURE_CONNECTION_SECURED = "email.smtp_secure_connection.secured";
  static final String EMAIL_SMTP_USERNAME_SECURED = "email.smtp_username.secured";
  static final String EMAIL_SMTP_PASSWORD_SECURED = "email.smtp_password.secured";
  static final String EMAIL_FROM = "email.from";
  static final String EMAIL_FROM_NAME = "email.fromName";
  static final String EMAIL_PREFIX = "email.prefix";
  static final String EMAIL_AUTH_METHOD = "email.smtp.auth.method";

  @VisibleForTesting
  static final List<String> SMTP_LEGACY_CONFIG_PROP_KEYS = List.of(
    EMAIL_SMTP_HOST_SECURED,
    EMAIL_SMTP_PORT_SECURED,
    EMAIL_SMTP_SECURE_CONNECTION_SECURED,
    EMAIL_SMTP_USERNAME_SECURED,
    EMAIL_SMTP_PASSWORD_SECURED,
    EMAIL_FROM,
    EMAIL_FROM_NAME,
    EMAIL_PREFIX,
    EMAIL_AUTH_METHOD
  );

  private static final Map<String, String> defaultPropertyValues = Map.of(
    EMAIL_SMTP_SECURE_CONNECTION_SECURED, "NONE",
    EMAIL_FROM, "noreply@nowhere",
    EMAIL_FROM_NAME, "SonarQube",
    EMAIL_PREFIX, "[SONARQUBE]",
    EMAIL_AUTH_METHOD, "BASIC"
  );

  private static final String PLACEHOLDER = "LIST_PLACEHOLDER";
  private static final String SELECT_PROPERTIES_QUERY = """
    select prop_key, is_empty, text_value, created_at from properties
    where prop_key in (LIST_PLACEHOLDER)
    """;
  private static final String DELETE_PROPERTIES_QUERY = """
    delete from properties
    where prop_key in (LIST_PLACEHOLDER)
    """;

  private static final String INSERT_INTERNAL_PROPERTIES_QUERY = """
    insert into internal_properties (kee, is_empty, text_value, created_at)
    values (?, ?, ?, ?)
    """;

  private final System2 system2;

  public MigrateSmtpConfiguration(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Map<String, PropertyDb> keyToProperties = new HashMap<>();
    String selectQuery = getQueryWithResolvedPlaceholder(SELECT_PROPERTIES_QUERY);
    context.prepareSelect(selectQuery).scroll(row -> keyToProperties.put(row.getString(1), getPropertyFromRow(row)));
    if (!keyToProperties.isEmpty()) {
      insertPropertiesIntoInternal(context, keyToProperties);
      deleteOriginalProperties(context);
      LOGGER.info("SMTP configuration properties successfully migrated into internal_properties");
    }
  }

  private static PropertyDb getPropertyFromRow(Select.Row row) throws SQLException {
    return new PropertyDb(
      row.getString(1),
      row.getBoolean(2),
      row.getString(3),
      row.getLong(4)
    );
  }

  private void insertPropertiesIntoInternal(Context context, Map<String, PropertyDb> properties) throws SQLException {
    addDefaultPropertiesIfNeeded(properties);
    properties.put(EMAIL_SMTP_SECURE_CONNECTION_SECURED, getSecureConnectionWithNewValues(properties.get(EMAIL_SMTP_SECURE_CONNECTION_SECURED)));
    Upsert insertInternalProperties = context.prepareUpsert(INSERT_INTERNAL_PROPERTIES_QUERY);
    for (PropertyDb property : properties.values()) {
      insertInternalProperties
        .setString(1, property.key)
        .setBoolean(2, property.isEmpty)
        .setString(3, property.value)
        .setLong(4, property.createdAt)
        .addBatch();
      LOGGER.debug("Migrated property: {}", property.key);
    }
    insertInternalProperties.execute().commit();
  }

  private void addDefaultPropertiesIfNeeded(Map<String, PropertyDb> keyToProperties) {
    defaultPropertyValues.forEach((key, value) -> {
      if (!keyToProperties.containsKey(key)) {
        keyToProperties.put(key, new PropertyDb(key, false, value, system2.now()));
      }
    });
  }

  private static PropertyDb getSecureConnectionWithNewValues(PropertyDb currentProperty) {
    String newValue = switch (currentProperty.value) {
      case "ssl" -> "SSLTLS";
      case "starttls" -> "STARTTLS";
      default -> "NONE";
    };
    return new PropertyDb(currentProperty.key, currentProperty.isEmpty, newValue, currentProperty.createdAt);
  }

  private static void deleteOriginalProperties(Context context) throws SQLException {
    context.prepareUpsert(getQueryWithResolvedPlaceholder(DELETE_PROPERTIES_QUERY))
      .execute()
      .commit();
  }

  private static String getQueryWithResolvedPlaceholder(String query) {
    return query.replace(PLACEHOLDER, SMTP_LEGACY_CONFIG_PROP_KEYS.stream().map(key -> "'" + key + "'").collect(joining(",")));
  }

  private record PropertyDb(
    String key,
    boolean isEmpty,
    String value,
    long createdAt
  ) {}
}
