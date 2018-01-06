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
package org.sonar.server.platform;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Settings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

public class PersistentSettings {

  private final Settings delegate;
  private final DbClient dbClient;
  private final SettingsChangeNotifier changeNotifier;

  public PersistentSettings(Settings delegate, DbClient dbClient, SettingsChangeNotifier changeNotifier) {
    this.delegate = delegate;
    this.dbClient = dbClient;
    this.changeNotifier = changeNotifier;
  }

  @CheckForNull
  public String getString(String key) {
    return delegate.getString(key);
  }

  /**
   * Insert property into database if value is not {@code null}, else delete property from
   * database. Session is not committed but {@link org.sonar.api.config.GlobalPropertyChangeHandler}
   * are executed.
   */
  public PersistentSettings saveProperty(DbSession dbSession, String key, @Nullable String value) {
    savePropertyImpl(dbSession, key, value);
    changeNotifier.onGlobalPropertyChange(key, value);
    return this;
  }

  /**
   * Same as {@link #saveProperty(DbSession, String, String)} but a new database session is
   * opened and committed.
   */
  public PersistentSettings saveProperty(String key, @Nullable String value) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      savePropertyImpl(dbSession, key, value);
      dbSession.commit();
      changeNotifier.onGlobalPropertyChange(key, value);
      return this;
    }
  }

  private void savePropertyImpl(DbSession dbSession, String key, @Nullable String value) {
    if (value == null) {
      dbClient.propertiesDao().deleteGlobalProperty(key, dbSession);
    } else {
      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(key).setValue(value));
    }
    // refresh the cache of settings
    delegate.setProperty(key, value);
  }

  public Settings getSettings() {
    return delegate;
  }
}
