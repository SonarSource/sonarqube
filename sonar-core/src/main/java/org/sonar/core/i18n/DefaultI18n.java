/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.picocontainer.Startable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class DefaultI18n implements I18n, Startable {

  private static final Logger LOG = Loggers.get(DefaultI18n.class);
  private static final String BUNDLE_PACKAGE = "org.sonar.l10n.";

  private final PluginRepository pluginRepository;
  private final ResourceBundle.Control control;
  private final System2 system2;

  // the following fields are available after startup
  private ClassLoader classloader;
  private Map<String, String> propertyToBundles;

  public DefaultI18n(PluginRepository pluginRepository) {
    this(pluginRepository, System2.INSTANCE);
  }

  @VisibleForTesting
  DefaultI18n(PluginRepository pluginRepository, System2 system2) {
    this.pluginRepository = pluginRepository;
    this.system2 = system2;
    // SONAR-2927
    this.control = new ResourceBundle.Control() {
      @Override
      public Locale getFallbackLocale(String baseName, Locale locale) {
        Preconditions.checkNotNull(baseName);
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
  void doStart(ClassLoader classloader) {
    this.classloader = classloader;
    this.propertyToBundles = new HashMap<>();
    Collection<PluginInfo> infos = pluginRepository.getPluginInfos();
    if (infos.isEmpty()) {
      addPlugin("core");
    } else {
      for (PluginInfo plugin : infos) {
        addPlugin(plugin.getKey());
      }
    }
    LOG.debug("Loaded {} properties from l10n bundles", propertyToBundles.size());
  }

  private void addPlugin(String pluginKey) {
    try {
      String bundleKey = BUNDLE_PACKAGE + pluginKey;
      ResourceBundle bundle = ResourceBundle.getBundle(bundleKey, Locale.ENGLISH, this.classloader, control);
      Enumeration<String> keys = bundle.getKeys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement();
        propertyToBundles.put(key, bundleKey);
      }
    } catch (MissingResourceException e) {
      // ignore
    }
  }

  @Override
  public void stop() {
    if (classloader instanceof Closeable) {
      IOUtils.closeQuietly((Closeable)classloader);
    }
    classloader = null;
    propertyToBundles = null;
  }

  @Override
  @CheckForNull
  public String message(Locale locale, String key, @Nullable String defaultValue, Object... parameters) {
    String bundleKey = propertyToBundles.get(key);
    String value = null;
    if (bundleKey != null) {
      try {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(bundleKey, locale, classloader, control);
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

  @Override
  public String age(Locale locale, long durationInMillis) {
    DurationLabel.Result duration = DurationLabel.label(durationInMillis);
    return message(locale, duration.key(), null, duration.value());
  }

  @Override
  public String age(Locale locale, Date fromDate, Date toDate) {
    return age(locale, toDate.getTime() - fromDate.getTime());
  }

  @Override
  public String ageFromNow(Locale locale, Date date) {
    return age(locale, system2.now() - date.getTime());
  }

  /**
   * Format date for the given locale. JVM timezone is used.
   */
  @Override
  public String formatDateTime(Locale locale, Date date) {
    return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, locale).format(date);
  }

  @Override
  public String formatDate(Locale locale, Date date) {
    return DateFormat.getDateInstance(DateFormat.DEFAULT, locale).format(date);
  }

  @Override
  public String formatDouble(Locale locale, Double value) {
    NumberFormat format = DecimalFormat.getNumberInstance(locale);
    format.setMinimumFractionDigits(1);
    format.setMaximumFractionDigits(1);
    return format.format(value);
  }

  @Override
  public String formatInteger(Locale locale, Integer value) {
    return NumberFormat.getNumberInstance(locale).format(value);
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
    InputStream input = classloader.getResourceAsStream(filePath);
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

  public Set<String> getPropertyKeys() {
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
    return classloader;
  }
}
