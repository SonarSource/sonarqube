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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.i18n.I18n;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.utils.SonarException;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

public class I18nManager implements I18n, ServerExtension {
  private static final Logger LOG = LoggerFactory.getLogger(I18nManager.class);

  public static final String ENGLISH_PACK_PLUGIN_KEY = "l10nen";
  public static final String BUNDLE_PACKAGE = "org.sonar.l10n.";

  private PluginRepository pluginRepository;
  private Map<String, ClassLoader> bundleToClassloaders;
  private Map<String, String> propertyToBundles;
  private ClassLoader languagePackClassLoader;
  private Map<String,Map<Locale,String>> fileContentCache = Maps.newHashMap();

  public I18nManager(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  I18nManager(Map<String, ClassLoader> bundleToClassloaders) {
    this.bundleToClassloaders = bundleToClassloaders;
  }

  public void start() {
    initClassloaders();
    initProperties();
  }

  private void initClassloaders() {
    if (bundleToClassloaders == null) {
      languagePackClassLoader = pluginRepository.getPlugin(ENGLISH_PACK_PLUGIN_KEY).getClass().getClassLoader();
      bundleToClassloaders = Maps.newHashMap();
      for (PluginMetadata metadata : pluginRepository.getMetadata()) {
        if (!metadata.isCore() && !ENGLISH_PACK_PLUGIN_KEY.equals(metadata.getBasePlugin())) {
          // plugin but not a language pack
          // => plugins embedd only their own bundles with all locales
          ClassLoader classLoader = pluginRepository.getPlugin(metadata.getKey()).getClass().getClassLoader();
          bundleToClassloaders.put(BUNDLE_PACKAGE + metadata.getKey(), classLoader);

        } else if (metadata.isCore()) {
          // bundles of core plugins are defined into language packs. All language packs are supposed
          // to share the same classloader (english pack classloader)
          bundleToClassloaders.put(BUNDLE_PACKAGE + metadata.getKey(), languagePackClassLoader);
        }
      }
    }
    bundleToClassloaders = Collections.unmodifiableMap(bundleToClassloaders);
  }

  private void initProperties() {
    propertyToBundles = Maps.newHashMap();
    for (Map.Entry<String, ClassLoader> entry : bundleToClassloaders.entrySet()) {
      try {
        String bundleKey = entry.getKey();
        ResourceBundle bundle = ResourceBundle.getBundle(bundleKey, Locale.ENGLISH, entry.getValue());
        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement();
          propertyToBundles.put(key, bundleKey);
        }
      } catch (MissingResourceException e) {
        // ignore
      }
    }
    propertyToBundles = Collections.unmodifiableMap(propertyToBundles);
    LOG.debug(String.format("Loaded %d properties from English bundles", propertyToBundles.size()));
  }

  public String message(Locale locale, String key, String defaultValue, Object... parameters) {
    String bundleKey = propertyToBundles.get(key);
    ResourceBundle resourceBundle = getBundle(bundleKey, locale);
    return message(resourceBundle, key, defaultValue, parameters);
  }

  /**
   * Only the given locale is searched. Contrary to java.util.ResourceBundle, no strategy for locating the bundle is implemented in
   * this method. 
   */
  String messageFromFile(Locale locale, String filename, String relatedProperty, boolean keepInCache) {
    Map<Locale,String> fileCache = fileContentCache.get(filename);
    if (fileCache!=null && fileCache.containsKey(locale)) {
      return fileCache.get(locale);
    }

    ClassLoader classloader = getClassLoaderForProperty(relatedProperty);
    String result = null;
    if (classloader != null) {
      String bundleBase = propertyToBundles.get(relatedProperty);
      String filePath = bundleBase.replace('.', '/');
      if (!"en".equals(locale.getLanguage())) {
        filePath += "_" + locale.getLanguage();
      }
      filePath += "/" + filename;
      InputStream input = classloader.getResourceAsStream(filePath);
      if (input != null) {
        try {
          result = IOUtils.toString(input, "UTF-8");
          if (keepInCache && result!=null) {
            if (fileCache==null) {
              fileCache = Maps.newHashMap();
              fileContentCache.put(filename, fileCache);
            }
            fileCache.put(locale, result);
          }
        } catch (IOException e) {
          throw new SonarException("Fail to load file: " + filePath, e);
        } finally {
          IOUtils.closeQuietly(input);
        }
      }
    }
    return result;
  }

  Set<String> getPropertyKeys() {
    return propertyToBundles.keySet();
  }

  ResourceBundle getBundle(String bundleKey, Locale locale) {
    try {
      ClassLoader classloader = bundleToClassloaders.get(bundleKey);
      if (classloader != null) {
        return ResourceBundle.getBundle(bundleKey, locale, classloader);
      }
    } catch (MissingResourceException e) {
      // ignore
    }
    return null;
  }


  ClassLoader getClassLoaderForProperty(String propertyKey) {
    String bundleKey = propertyToBundles.get(propertyKey);
    return (bundleKey != null ? bundleToClassloaders.get(bundleKey) : null);
  }

  String message(ResourceBundle resourceBundle, String key, String defaultValue, Object... parameters) {
    String value = null;
    if (resourceBundle != null) {
      try {
        value = resourceBundle.getString(key);
      } catch (MissingResourceException e) {
        // ignore
      }
    }
    if (value == null) {
      value = defaultValue;
    }
    if (value != null && parameters.length > 0) {
      return MessageFormat.format(value, parameters);
    }
    return value;
  }

  String extractBundleFromKey(String key) {
    String bundleKey = BUNDLE_PACKAGE + StringUtils.substringBefore(key, ".");
    if (bundleToClassloaders.containsKey(bundleKey)) {
      return bundleKey;
    }
    return BUNDLE_PACKAGE + "core";
  }

  ClassLoader getLanguagePackClassLoader() {
    return languagePackClassLoader;
  }

  Map<String, Map<Locale, String>> getFileContentCache() {
    return fileContentCache;
  }
}
