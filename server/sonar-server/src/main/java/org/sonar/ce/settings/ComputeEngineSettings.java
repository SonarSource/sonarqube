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
package org.sonar.ce.settings;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.ServerSettings;

import static com.google.common.base.Preconditions.checkState;

/**
 * This class implements ServerSettings and extends Settings so that it can be injected in any component depending upon
 * either ServerSettings or Settings.
 *
 * <p>
 * In order to honor both Settings and ThreadLocalSettings contracts, none of the code inherited from the Settings super
 * class is actually used. Every public method of Settings is override and their implementation is delegated to
 * an inner object which can either be the default one or one specific to the current Thread. Selected of the inner
 * object will depend on whether the current Thread made use of method {@link #load()} or not. This approach also greatly
 * simplifies delegation code (see {@link #currentDelegate()}).
 * </p>
 */
public class ComputeEngineSettings extends Settings implements ThreadLocalSettings, ServerSettings {
  private final ServerSettings defaultDelegate;
  private final ThreadLocal<ServerSettings> threadLocalDelegate = new ThreadLocal<>();

  private final Properties rootProperties;
  private final ComponentContainer componentContainer;
  // we can't get injected with DBClient because it creates a circular dependency
  private volatile DbClient dbClient;

  public ComputeEngineSettings(PropertyDefinitions definitions, Properties rootProperties, ComponentContainer componentContainer) {
    super(definitions);
    this.rootProperties = rootProperties;
    this.componentContainer = componentContainer;

    this.defaultDelegate = new ServerSettingsImpl(definitions, rootProperties);
  }

  @Override
  public void load() {
    checkState(
      this.threadLocalDelegate.get() == null,
      "loadLocal called twice for Thread '%s' or state wasn't cleared last time it was used",
      Thread.currentThread().getName());
    this.threadLocalDelegate.set(loadServerSettings());
  }

  @Override
  public void unload() {
    this.threadLocalDelegate.remove();
  }

  private ServerSettings loadServerSettings() {
    ServerSettings res = new ServerSettingsImpl(this.definitions, this.rootProperties);
    Map<String, String> databaseProperties = Maps.newHashMap();
    for (PropertyDto property : getDbClient().propertiesDao().selectGlobalProperties()) {
      databaseProperties.put(property.getKey(), property.getValue());
    }
    res.activateDatabaseSettings(databaseProperties);
    return res;
  }

  private DbClient getDbClient() {
    if (dbClient == null) {
      this.dbClient = componentContainer.getComponentByType(DbClient.class);
    }
    return dbClient;
  }

  private ServerSettings currentDelegate() {
    return MoreObjects.firstNonNull(threadLocalDelegate.get(), defaultDelegate);
  }

  private Settings currentSettings() {
    return currentDelegate().getSettings();
  }

  @Override
  public ServerSettings activateDatabaseSettings(Map<String, String> databaseProperties) {
    checkState(threadLocalDelegate.get() == null, "activateDatabaseSettings must not be called from a Worker");

    return defaultDelegate.activateDatabaseSettings(databaseProperties);
  }

  private static final class ServerSettingsImpl extends Settings implements ServerSettings {

    private final Properties rootProperties;

    public ServerSettingsImpl(PropertyDefinitions definitions, Properties rootProperties) {
      super(definitions);
      this.rootProperties = rootProperties;
      super.addProperties(rootProperties);
      // Secret key is loaded from conf/sonar.properties
      super.getEncryption().setPathToSecretKey(super.getString(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    }

    @Override
    public ServerSettings activateDatabaseSettings(Map<String, String> databaseProperties) {
      super.clear();

      // order is important : the last override the first
      super.addProperties(databaseProperties);
      super.addProperties(rootProperties);

      return this;
    }

    @Override
    public Settings getSettings() {
      return this;
    }
  }

  @Override
  public Settings getSettings() {
    return this;
  }

  @Override
  public Encryption getEncryption() {
    return currentSettings().getEncryption();
  }

  @Override
  @CheckForNull
  public String getDefaultValue(String key) {
    return currentSettings().getDefaultValue(key);
  }

  @Override
  public boolean hasKey(String key) {
    return currentSettings().hasKey(key);
  }

  @Override
  public boolean hasDefaultValue(String key) {
    return currentSettings().hasDefaultValue(key);
  }

  @Override
  @CheckForNull
  public String getString(String key) {
    return currentDelegate().getString(key);
  }

  @Override
  public boolean getBoolean(String key) {
    return currentSettings().getBoolean(key);
  }

  @Override
  public int getInt(String key) {
    return currentSettings().getInt(key);
  }

  @Override
  public long getLong(String key) {
    return currentSettings().getLong(key);
  }

  @Override
  @CheckForNull
  public Date getDate(String key) {
    return currentSettings().getDate(key);
  }

  @Override
  @CheckForNull
  public Date getDateTime(String key) {
    return currentSettings().getDateTime(key);
  }

  @Override
  @CheckForNull
  public Float getFloat(String key) {
    return currentSettings().getFloat(key);
  }

  @Override
  @CheckForNull
  public Double getDouble(String key) {
    return currentSettings().getDouble(key);
  }

  @Override
  public String[] getStringArray(String key) {
    return currentSettings().getStringArray(key);
  }

  @Override
  public String[] getStringLines(String key) {
    return currentSettings().getStringLines(key);
  }

  @Override
  public String[] getStringArrayBySeparator(String key, String separator) {
    return currentSettings().getStringArrayBySeparator(key, separator);
  }

  @Override
  public List<String> getKeysStartingWith(String prefix) {
    return currentSettings().getKeysStartingWith(prefix);
  }

  @Override
  public Settings appendProperty(String key, String value) {
    return currentSettings().appendProperty(key, value);
  }

  @Override
  public Settings setProperty(String key, @Nullable String[] values) {
    return currentSettings().setProperty(key, values);
  }

  @Override
  public Settings setProperty(String key, @Nullable String value) {
    return currentSettings().setProperty(key, value);
  }

  @Override
  public Settings setProperty(String key, @Nullable Boolean value) {
    return currentSettings().setProperty(key, value);
  }

  @Override
  public Settings setProperty(String key, @Nullable Integer value) {
    return currentSettings().setProperty(key, value);
  }

  @Override
  public Settings setProperty(String key, @Nullable Long value) {
    return currentSettings().setProperty(key, value);
  }

  @Override
  public Settings setProperty(String key, @Nullable Double value) {
    return currentSettings().setProperty(key, value);
  }

  @Override
  public Settings setProperty(String key, @Nullable Float value) {
    return currentSettings().setProperty(key, value);
  }

  @Override
  public Settings setProperty(String key, @Nullable Date date) {
    return currentSettings().setProperty(key, date);
  }

  @Override
  public Settings addProperties(Map<String, String> props) {
    return currentSettings().addProperties(props);
  }

  @Override
  public Settings addProperties(Properties props) {
    return currentSettings().addProperties(props);
  }

  @Override
  public Settings setProperties(Map<String, String> props) {
    return currentSettings().setProperties(props);
  }

  @Override
  public Settings setProperty(String key, @Nullable Date date, boolean includeTime) {
    return currentSettings().setProperty(key, date, includeTime);
  }

  @Override
  public Settings removeProperty(String key) {
    return currentSettings().removeProperty(key);
  }

  @Override
  public Settings clear() {
    return currentSettings().clear();
  }

  @Override
  public Map<String, String> getProperties() {
    return currentSettings().getProperties();
  }

  @Override
  public PropertyDefinitions getDefinitions() {
    return currentSettings().getDefinitions();
  }

}
