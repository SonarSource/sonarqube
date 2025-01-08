/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_AUTH_METHOD;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_FROM;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_FROM_NAME;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_PREFIX;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_SMTP_HOST_SECURED;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_SMTP_PASSWORD_SECURED;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_SMTP_PORT_SECURED;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_SMTP_SECURE_CONNECTION_SECURED;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.EMAIL_SMTP_USERNAME_SECURED;
import static org.sonar.server.platform.db.migration.version.v107.MigrateSmtpConfiguration.SMTP_LEGACY_CONFIG_PROP_KEYS;

class MigrateSmtpConfigurationIT {

  public static final String RANDOM_PROPERTY_KEY = "random.property";
  public static final String RANDOM_PROPERTY_VALUE = "random value";
  public static final String RANDOM_INTERNAL_PROPERTY_KEY = "random.internal.property";
  public static final String RANDOM_INTERNAL_PROPERTY_VALUE = "random internal value";

  @RegisterExtension
  public final LogTesterJUnit5 logger = new LogTesterJUnit5();

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateSmtpConfiguration.class);

  private final System2 system2 = mock();
  private final DataChange underTest = new MigrateSmtpConfiguration(db.database(), system2);

  @BeforeEach
  public void before() {
    logger.clear();
  }

  @Test
  void execute_whenNoSmtpConfig_shouldDoNothing() throws SQLException {
    insertRandomProperty();
    insertRandomInternalProperty();

    underTest.execute();

    assertNoInternalPropertiesHadBeenAdded();
    assertOtherPropertiesLeftUntouched();
  }

  private void assertNoInternalPropertiesHadBeenAdded() {
    Map<String, String> dbRows = new HashMap<>();
    db.select(format("select kee, text_value from internal_properties where kee in (%s)", getPropertyKeysAsSqlList()))
      .forEach(map -> dbRows.put((String) map.get("kee"), (String) map.get("text_value")));
    assertThat(dbRows).isEmpty();
  }

  @Test
  void execute_whenPartialSmtpConfig_shouldMigrate() throws SQLException {
    Map<String, String> smtpProperties = new HashMap<>();
    smtpProperties.put(EMAIL_SMTP_HOST_SECURED, "host");
    smtpProperties.put(EMAIL_SMTP_USERNAME_SECURED, "username");
    smtpProperties.put(EMAIL_FROM, "from");

    insertProperties(smtpProperties);
    insertRandomProperty();
    insertRandomInternalProperty();

    underTest.execute();

    assertThatPropertiesAreMigrated(smtpProperties);
    assertOtherPropertiesLeftUntouched();
  }

  @Test
  void execute_whenFullSmtpConfig_shouldMigrate() throws SQLException {
    Map<String, String> smtpProperties = new HashMap<>();
    smtpProperties.put(EMAIL_SMTP_HOST_SECURED, "host");
    smtpProperties.put(EMAIL_SMTP_PORT_SECURED, "port");
    smtpProperties.put(EMAIL_SMTP_SECURE_CONNECTION_SECURED, "secure connection");
    smtpProperties.put(EMAIL_SMTP_USERNAME_SECURED, "username");
    smtpProperties.put(EMAIL_SMTP_PASSWORD_SECURED, "password");
    smtpProperties.put(EMAIL_FROM, "from");
    smtpProperties.put(EMAIL_FROM_NAME, "name");
    smtpProperties.put(EMAIL_PREFIX, "prefix");

    insertProperties(smtpProperties);
    insertRandomProperty();
    insertRandomInternalProperty();

    underTest.execute();

    assertThatPropertiesAreMigrated(smtpProperties);
    assertOtherPropertiesLeftUntouched();
  }

  @Test
  void execute_whenDefaultValuesUsed_shouldDefineThem() throws SQLException {
    Map<String, String> smtpProperties = new HashMap<>();
    smtpProperties.put(EMAIL_SMTP_HOST_SECURED, "host");

    insertProperties(smtpProperties);

    underTest.execute();

    // This method adds all default values to the properties list
    assertThatPropertiesAreMigrated(smtpProperties);
  }

  @ParameterizedTest
  @MethodSource("secureConnectionOldToNewValues")
  void execute_shouldMapSecureConnectionValues(String oldValue, String newValue) throws SQLException {
    Map<String, String> smtpProperties = new HashMap<>();
    smtpProperties.put(EMAIL_SMTP_SECURE_CONNECTION_SECURED, oldValue);

    insertProperties(smtpProperties);

    underTest.execute();

    assertSecureConnectionValuesIsCorrectlyMapped(newValue);
  }

  private void assertSecureConnectionValuesIsCorrectlyMapped(String newValue) {
    Map<String, String> dbRows = new HashMap<>();
    db.select(format("select kee, text_value from internal_properties where kee = '%s'", EMAIL_SMTP_SECURE_CONNECTION_SECURED))
      .forEach(map -> dbRows.put((String) map.get("kee"), (String) map.get("text_value")));
    assertThat(dbRows).containsEntry(EMAIL_SMTP_SECURE_CONNECTION_SECURED, newValue);
  }

  static Object[][] secureConnectionOldToNewValues() {
    return new Object[][]{
      {"ssl", "SSLTLS"},
      {"starttls", "STARTTLS"},
      {null, "NONE"},
      {"", "NONE"},
      {"null", "NONE"},
      {"random", "NONE"}
    };
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    Map<String, String> smtpProperties = new HashMap<>();
    smtpProperties.put(EMAIL_SMTP_HOST_SECURED, "host");
    smtpProperties.put(EMAIL_SMTP_PORT_SECURED, "port");
    smtpProperties.put(EMAIL_SMTP_SECURE_CONNECTION_SECURED, "secure connection");
    smtpProperties.put(EMAIL_SMTP_USERNAME_SECURED, "username");
    smtpProperties.put(EMAIL_SMTP_PASSWORD_SECURED, "password");
    smtpProperties.put(EMAIL_FROM, "from");
    smtpProperties.put(EMAIL_FROM_NAME, "name");
    smtpProperties.put(EMAIL_PREFIX, "prefix");

    insertProperties(smtpProperties);
    insertRandomProperty();
    insertRandomInternalProperty();

    underTest.execute();
    underTest.execute();

    assertThatPropertiesAreMigrated(smtpProperties);
    assertOtherPropertiesLeftUntouched();
  }

  private void insertRandomProperty() {
    insertProperty(RANDOM_PROPERTY_KEY, RANDOM_PROPERTY_VALUE);
  }

  private void insertRandomInternalProperty() {
    insertInternalProperty(RANDOM_INTERNAL_PROPERTY_KEY, RANDOM_INTERNAL_PROPERTY_VALUE);
  }

  private void assertThatPropertiesAreMigrated(Map<String, String> properties) {
    addDefaultProperties(properties);
    updatePropertyValues(properties);
    assertThatPropertiesAreInInternalProperties(properties);
    assertThatPropertiesAreNotInProperties();
  }

  private void updatePropertyValues(Map<String, String> properties) {
    String currentValue = properties.get(EMAIL_SMTP_SECURE_CONNECTION_SECURED);
    String newValue = switch (currentValue) {
      case "ssl" -> "SSLTLS";
      case "starttls" -> "STARTTLS";
      default -> "NONE";
    };
    properties.put(EMAIL_SMTP_SECURE_CONNECTION_SECURED, newValue);
  }

  private void addDefaultProperties(Map<String, String> properties) {
    Map<String, String> defaultPropertyValues = Map.of(
      EMAIL_SMTP_SECURE_CONNECTION_SECURED, "NONE",
      EMAIL_FROM, "noreply@nowhere",
      EMAIL_FROM_NAME, "SonarQube",
      EMAIL_PREFIX, "[SONARQUBE]",
      EMAIL_AUTH_METHOD, "BASIC"
    );
    defaultPropertyValues.forEach((key, value) -> {
      if (!properties.containsKey(key)) {
        properties.put(key, value);
      }
    });
  }

  private void assertThatPropertiesAreInInternalProperties(Map<String, String> properties) {
    Map<String, String> dbRows = new HashMap<>();
    db.select(format("select kee, text_value from internal_properties where kee in (%s)", getPropertyKeysAsSqlList()))
      .forEach(map -> dbRows.put((String) map.get("kee"), (String) map.get("text_value")));
    assertThat(dbRows).containsExactlyInAnyOrderEntriesOf(properties);
  }

  private void assertThatPropertiesAreNotInProperties() {
    assertThat(db.select(format("select * from properties where prop_key in (%s)", getPropertyKeysAsSqlList()))).isEmpty();
  }

  private static String getPropertyKeysAsSqlList() {
    return SMTP_LEGACY_CONFIG_PROP_KEYS.stream().map(key -> "'" + key + "'").collect(joining(","));
  }

  private void assertOtherPropertiesLeftUntouched() {
    assertRandomPropertyIsIntact();
    assertRandomInternalPropertyIsIntact();
  }

  private void assertRandomPropertyIsIntact() {
    List<Tuple> results = db.select("select * from properties")
      .stream().map(map -> new Tuple(map.get("prop_key"), map.get("text_value")))
      .toList();
    assertThat(results).containsExactly(new Tuple(RANDOM_PROPERTY_KEY, RANDOM_PROPERTY_VALUE));
  }

  private void assertRandomInternalPropertyIsIntact() {
    List<Tuple> resultsInternal = db.select(format("select kee, text_value from internal_properties where kee in ('%s')", RANDOM_INTERNAL_PROPERTY_KEY))
      .stream().map(map -> new Tuple(map.get("kee"), map.get("text_value")))
      .toList();
    assertThat(resultsInternal).containsExactly(new Tuple(RANDOM_INTERNAL_PROPERTY_KEY, RANDOM_INTERNAL_PROPERTY_VALUE));
  }

  private void insertProperties(Map<String, String> properties) {
    properties.forEach(this::insertProperty);
  }

  private void insertProperty(String key, String value) {
    db.executeInsert("properties", "uuid", "uuid_" + key.substring(0, min(key.length() - 1, 35)), "prop_key", key, "is_empty", false, "text_value", value, "created_at", 0);
  }

  private void insertInternalProperty(String key, String value) {
    db.executeInsert("internal_properties", "kee", key, "is_empty", false, "text_value", value, "created_at", 0);
  }

}
