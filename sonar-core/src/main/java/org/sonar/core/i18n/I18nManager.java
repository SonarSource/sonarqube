/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.i18n;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.i18n.I18n;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.utils.SonarException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class I18nManager implements I18n, ServerExtension, BatchExtension, Startable {
  private static final Logger LOG = LoggerFactory.getLogger(I18nManager.class);

  public static final String BUNDLE_PACKAGE = "org.sonar.l10n.";

  private PluginRepository pluginRepository;
  private I18nClassloader i18nClassloader;
  private Map<String, String> propertyToBundles;
  private final ResourceBundle.Control control;

  public I18nManager(PluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
    // SONAR-2927
    this.control = new ResourceBundle.Control() {
      @Override
      public Locale getFallbackLocale(String baseName, Locale locale) {
        if (baseName == null) {
          throw new NullPointerException();
        }
        Locale defaultLocale = Locale.ENGLISH;
        return locale.equals(defaultLocale) ? null : defaultLocale;
      }
    };
  }

  @Override
  public void start() {
    doStart(new I18nClassloader(pluginRepository));
  }

  @VisibleForTesting
  void doStart(I18nClassloader classloader) {
    this.i18nClassloader = classloader;
    propertyToBundles = Maps.newHashMap();
    for (PluginMetadata plugin : pluginRepository.getMetadata()) {
      try {
        String bundleKey = BUNDLE_PACKAGE + plugin.getKey();
        ResourceBundle bundle = ResourceBundle.getBundle(bundleKey, Locale.ENGLISH, i18nClassloader, control);
        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement();
          propertyToBundles.put(key, bundleKey);
        }
      } catch (MissingResourceException e) {
        // ignore
      }
    }
    LOG.debug(String.format("Loaded %d properties from l10n bundles", propertyToBundles.size()));
  }

  @Override
  public void stop() {
    i18nClassloader = null;
    propertyToBundles = null;
  }

  @CheckForNull
  public String message(Locale locale, String key, @Nullable String defaultValue, Object... parameters) {
    String bundleKey = propertyToBundles.get(key);
    String value = null;
    if (bundleKey != null) {
      try {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(bundleKey, locale, i18nClassloader, control);
        value = resourceBundle.getString(key);
      } catch (MissingResourceException e1) {
        // ignore
      }
    }
    if (value == null) {
      value = defaultValue;
    }
    return formatMessage(value, parameters);
  }

  /**
   * Only the given locale is searched. Contrary to java.util.ResourceBundle, no strategy for locating the bundle is implemented in
   * this method.
   */
  String messageFromFile(Locale locale, String filename, String relatedProperty) {
    String result = null;
    String bundleBase = propertyToBundles.get(relatedProperty);
    if (bundleBase == null) {
      // this property has no translation
      return null;
    }

    String filePath = bundleBase.replace('.', '/');
    if (!"en".equals(locale.getLanguage())) {
      filePath += "_" + locale.getLanguage();
    }
    filePath += "/" + filename;
    InputStream input = i18nClassloader.getResourceAsStream(filePath);
    if (input != null) {
      result = readInputStream(filePath, input);
    }
    return result;
  }

  String readInputStream(String filePath, InputStream input) {
    String result = null;
    try {
      result = IOUtils.toString(input, "UTF-8");
    } catch (IOException e) {
      throw new SonarException("Fail to load file: " + filePath, e);
    } finally {
      IOUtils.closeQuietly(input);
    }
    return result;
  }

  @VisibleForTesting
  Set<String> getPropertyKeys() {
    return propertyToBundles.keySet();
  }

  @CheckForNull
  private String formatMessage(@Nullable String message, Object... parameters) {
    if (message == null || parameters.length == 0) {
      return message;
    }
    return MessageFormat.format(message.replaceAll("'", "''"), parameters);
  }

  ClassLoader getBundleClassLoader() {
    return i18nClassloader;
  }
}
