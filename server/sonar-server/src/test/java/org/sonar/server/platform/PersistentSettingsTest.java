/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform;

import com.google.common.collect.ImmutableMap;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistentSettingsTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession = db.getSession();

  private PropertiesDao dao = dbClient.propertiesDao();
  private ServerSettings settings = new ServerSettingsImpl(
    new PropertyDefinitions(),
    new Properties());

  @Test
  public void load_database_properties_at_startup() {
    newGlobalProperty("in_db", "bar");

    PersistentSettings persistentSettings = new PersistentSettings(dbClient, settings);
    persistentSettings.start();

    assertThat(settings.getString("in_db")).isEqualTo("bar");
  }

  @Test
  public void saveProperty() {
    PersistentSettings persistentSettings = new PersistentSettings(dbClient, settings);
    persistentSettings.saveProperty("foo", "bar");

    // kept in memory cache and persisted in db
    assertThat(settings.getString("foo")).isEqualTo("bar");
    verifyGlobalPropertyExists("foo", "bar");
  }

  @Test
  public void deleteProperty() {
    newGlobalProperty("foo", "bar_in_db");
    settings.setProperty("foo", "bar");
    assertThat(settings.hasKey("foo")).isTrue();

    PersistentSettings persistentSettings = new PersistentSettings(dbClient, settings);
    persistentSettings.deleteProperty("foo");

    assertThat(settings.hasKey("foo")).isFalse();
    verifyGlobalPropertyDoesNotExist("foo");
  }

  @Test
  public void deleteProperties() {
    newGlobalProperty("in_db1", "foo");
    newGlobalProperty("in_db2", "bar");
    settings.setProperty("foo", "bar");
    assertThat(settings.hasKey("foo")).isTrue();

    PersistentSettings persistentSettings = new PersistentSettings(dbClient, settings);
    persistentSettings.deleteProperties();

    assertThat(settings.getProperties()).isEmpty();
    assertThat(dao.selectGlobalProperties()).isEmpty();
  }

  @Test
  public void shortcuts_on_settings() {
    settings.setProperty("foo", "bar");
    assertThat(settings.hasKey("foo")).isTrue();

    PersistentSettings persistentSettings = new PersistentSettings(dbClient, settings);

    assertThat(persistentSettings.getProperties()).isEqualTo(settings.getProperties());
    assertThat(persistentSettings.getString("foo")).isEqualTo("bar");
    assertThat(persistentSettings.getSettings()).isEqualTo(settings);
  }

  @Test
  public void saveProperties() {
    PersistentSettings persistentSettings = new PersistentSettings(dbClient, settings);
    ImmutableMap<String, String> props = ImmutableMap.of("foo", "bar");
    persistentSettings.saveProperties(props);

    assertThat(settings.getString("foo")).isEqualTo("bar");
    verifyGlobalPropertyExists("foo", "bar");
  }

  private PropertyDto newGlobalProperty(String key, String value) {
    PropertyDto propertyDto = new PropertyDto().setKey(key).setValue(value);
    dao.insertProperty(dbSession, propertyDto);
    dbSession.commit();
    return propertyDto;
  }

  private void verifyGlobalPropertyExists(String key, String value){
    PropertyDto propertyDto = dao.selectGlobalProperty(dbSession, key);
    assertThat(propertyDto).isNotNull();
    assertThat(propertyDto.getValue()).isEqualTo(value);
    assertThat(propertyDto.getUserId()).isNull();
    assertThat(propertyDto.getResourceId()).isNull();
  }

  private void verifyGlobalPropertyDoesNotExist(String key){
    assertThat(dao.selectGlobalProperty(dbSession, key)).isNull();
  }

}
