/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.i18n;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerExtension;
import org.sonar.api.i18n.I18n;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class I18nManager implements I18n, ServerExtension {

  private PluginRepository pluginRepository;
  private Map<String, ClassLoader> bundleToClassloader;

  public I18nManager(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  I18nManager(Map<String, ClassLoader> bundleToClassloader) {
    this.bundleToClassloader = bundleToClassloader;
  }

  public void start() {
    ClassLoader coreClassLoader = pluginRepository.getPlugin("i18nen").getClass().getClassLoader();

    bundleToClassloader = Maps.newHashMap();
    for (PluginMetadata metadata : pluginRepository.getMetadata()) {
      if (!metadata.isCore() && !"i18nen".equals(metadata.getBasePlugin())) {
        ClassLoader classLoader = pluginRepository.getPlugin(metadata.getKey()).getClass().getClassLoader();
        bundleToClassloader.put(metadata.getKey(), classLoader);

      } else if (metadata.isCore()) {
        bundleToClassloader.put(metadata.getKey(), coreClassLoader);
      }
    }
  }

  public String message(Locale locale, String key, String defaultValue, Object... parameters) {
    String bundle = keyToBundle(key);
    ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, locale, bundleToClassloader.get(bundle));
    String value = resourceBundle.getString(key);
    if (value==null) {
      value = defaultValue;
    }
    if (value != null && parameters.length > 0) {
      return MessageFormat.format(value, parameters);
    }
    return value;
  }

  String keyToBundle(String key) {
    String pluginKey = StringUtils.substringBefore(key, ".");
    if (bundleToClassloader.containsKey(pluginKey)) {
      return pluginKey;
    }
    return "core";
  }
}
