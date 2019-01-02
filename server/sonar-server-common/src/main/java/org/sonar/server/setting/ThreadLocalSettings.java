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
package org.sonar.server.setting;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import org.apache.ibatis.exceptions.PersistenceException;
import org.sonar.api.CoreProperties;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Merge of system settings (including conf/sonar.properties) and the global properties stored
 * in the db table "properties". These settings do not contain the settings specific to a project.
 *
 * <p>
 * System settings have precedence on others.
 * </p>
 *
 * <p>
 * The thread-local cache is optional. It is disabled when the method {@link #unload()} has not
 * been called. That allows to remove complexity with handling of cleanup of thread-local cache
 * on daemon threads (notifications) or startup "main" thread.
 * </p>
 */
@ComputeEngineSide
@ServerSide
public class ThreadLocalSettings extends Settings {
  private static final Logger LOG = Loggers.get(ThreadLocalSettings.class);

  private final Properties overwrittenSystemProps = new Properties();
  private final Properties systemProps = new Properties();
  private static final ThreadLocal<Map<String, String>> CACHE = new ThreadLocal<>();
  private Map<String, String> getPropertyDbFailureCache = Collections.emptyMap();
  private Map<String, String> getPropertiesDbFailureCache = Collections.emptyMap();
  private SettingLoader settingLoader;

  public ThreadLocalSettings(PropertyDefinitions definitions, Properties props) {
    this(definitions, props, new NopSettingLoader());
  }

  @VisibleForTesting
  ThreadLocalSettings(PropertyDefinitions definitions, Properties props, SettingLoader settingLoader) {
    super(definitions, new Encryption(null));
    this.settingLoader = settingLoader;
    props.forEach((k, v) -> systemProps.put(k, v == null ? null : v.toString().trim()));

    // TODO something wrong about lifecycle here. It could be improved
    getEncryption().setPathToSecretKey(props.getProperty(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
  }

  @VisibleForTesting
  SettingLoader getSettingLoader() {
    return settingLoader;
  }

  protected void setSettingLoader(SettingLoader settingLoader) {
    this.settingLoader = Objects.requireNonNull(settingLoader);
  }

  @Override
  protected Optional<String> get(String key) {
    // search for the first value available in
    // 1. overwritten system properties
    // 2. system properties
    // 3. thread local cache (if enabled)
    // 4. db

    String value =  overwrittenSystemProps.getProperty(key);
    if (value != null) {
      return Optional.of(value);
    }

    value = systemProps.getProperty(key);
    if (value != null) {
      return Optional.of(value);
    }

    Map<String, String> dbProps = CACHE.get();
    // caching is disabled
    if (dbProps == null) {
      return Optional.ofNullable(load(key));
    }

    String loadedValue;
    if (dbProps.containsKey(key)) {
      // property may not exist in db. In this case key is present
      // in cache but value is null
      loadedValue = dbProps.get(key);
    } else {
      // cache the effective value (null if the property
      // is not persisted)
      loadedValue = load(key);
      dbProps.put(key, loadedValue);
    }
    return Optional.ofNullable(loadedValue);
  }

  private String load(String key) {
    try {
      return settingLoader.load(key);
    } catch (PersistenceException e) {
      return getPropertyDbFailureCache.get(key);
    }
  }

  public void setSystemProperty(String key, String value) {
    checkKeyAndValue(key, value);
    String systemValue = systemProps.getProperty(key);
    if (LOG.isDebugEnabled() && systemValue != null && !value.equals(systemValue)) {
      LOG.debug(format("System property '%s' with value '%s' overwritten with value '%s'", key, systemValue, value));
    }
    overwrittenSystemProps.put(key, value.trim());
  }

  @Override
  protected void set(String key, String value) {
    checkKeyAndValue(key, value);
    Map<String, String> dbProps = CACHE.get();
    if (dbProps != null) {
      dbProps.put(key, value.trim());
    }
  }

  private static void checkKeyAndValue(String key, String value) {
    requireNonNull(key, "key can't be null");
    requireNonNull(value, "value can't be null");
  }

  @Override
  protected void remove(String key) {
    Map<String, String> dbProps = CACHE.get();
    if (dbProps != null) {
      dbProps.remove(key);
    }
  }

  /**
   * Enables the thread specific cache of settings.
   */
  public void load() {
    CACHE.set(new HashMap<>());
  }

  /**
   * Clears the cache specific to the current thread (if any).
   */
  public void unload() {
    Map<String, String> settings = CACHE.get();
    CACHE.remove();
    // update cache of settings to be used in case of DB connectivity error
    this.getPropertyDbFailureCache = settings;
  }

  @Override
  public Map<String, String> getProperties() {
    Map<String, String> result = new HashMap<>();
    loadAll(result);
    systemProps.forEach((key, value) -> result.put((String) key, (String) value));
    return unmodifiableMap(result);
  }

  private void loadAll(Map<String, String> appendTo) {
    try {
      Map<String, String> cache = settingLoader.loadAll();
      appendTo.putAll(cache);
      getPropertiesDbFailureCache = cache;
    } catch (PersistenceException e) {
      appendTo.putAll(getPropertiesDbFailureCache);
    }
  }
}
