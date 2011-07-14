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
package org.sonar.plugins.core.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.i18n.I18n;
import org.sonar.api.i18n.LanguagePack;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class I18nManager implements I18n, ServerExtension, BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(I18nManager.class);
  public static final String packagePathToSearchIn = "org/sonar/i18n";

  private PluginRepository pluginRepository;
  private LanguagePack[] languagePacks;
  private Map<String, String> keys = Maps.newHashMap();
  private Properties unknownKeys = new Properties();
  private BundleClassLoader bundleClassLoader = new BundleClassLoader();
  private List<Locale> registeredLocales = Lists.newArrayList();

  public I18nManager(PluginRepository pluginRepository, LanguagePack[] languagePacks) {
    this.pluginRepository = pluginRepository;
    this.languagePacks = languagePacks;
  }

  public I18nManager(PluginRepository pluginRepository) {
    this(pluginRepository, new LanguagePack[0]);
  }

  public void start() {
    doStart(InstalledPlugin.create(pluginRepository));
  }

  protected void doStart(List<InstalledPlugin> installedPlugins) {
    Logs.INFO.info("Loading i18n bundles");
    Set<URI> alreadyLoadedResources = Sets.newHashSet();
    LanguagePack englishPack = findEnglishPack();

    for (InstalledPlugin plugin : installedPlugins) {
      // look first in the classloader of the English I18n Plugin of the Sonar platform
      searchAndStoreBundleNames(plugin.key, englishPack.getClass().getClassLoader(), alreadyLoadedResources);
      // then look in the classloader of the plugin
      searchAndStoreBundleNames(plugin.key, plugin.classloader, alreadyLoadedResources);
    }

    for (LanguagePack pack : languagePacks) {
      if ( !pack.equals(englishPack)) {
        addLanguagePack(pack);
      }
    }
  }

  protected LanguagePack findEnglishPack() {
    LanguagePack englishPack = null;
    for (LanguagePack pack : languagePacks) {
      if (pack.getLocales().contains(Locale.ENGLISH)) {
        englishPack = pack;
        break;
      }
    }
    if (englishPack == null) {
      throw new SonarException("The I18n English Pack was not found.");
    }
    return englishPack;
  }

  protected void addLanguagePack(LanguagePack languagePack) {
    LOG.debug("Search for bundles in language pack : {}", languagePack);
    for (String pluginKey : languagePack.getPluginKeys()) {
      String bundleBaseName = buildBundleBaseName(pluginKey);
      for (Locale locale : languagePack.getLocales()) {
        String bundlePropertiesFile = new StringBuilder(bundleBaseName).append('_').append(locale.toString()).append(".properties")
            .toString();
        ClassLoader classloader = languagePack.getClass().getClassLoader();
        LOG.info("Adding locale {} for bundleName : {} from classloader : {}", new Object[] { locale, bundleBaseName, classloader });
        bundleClassLoader.addResource(bundlePropertiesFile, classloader);
        registeredLocales.add(locale);
      }
    }
  }

  protected String buildBundleBaseName(String pluginKey) {
    return packagePathToSearchIn + "/" + pluginKey;
  }

  @SuppressWarnings("unchecked")
  protected void searchAndStoreBundleNames(String pluginKey, ClassLoader classloader, Set<URI> alreadyLoadedResources) {
    String bundleBaseName = buildBundleBaseName(pluginKey);
    String bundleDefaultPropertiesFile = bundleBaseName + ".properties";
    try {
      LOG.debug("Search for ResourceBundle base file '" + bundleDefaultPropertiesFile + "' in the classloader : " + classloader);
      List<URL> resources = EnumerationUtils.toList(classloader.getResources(bundleDefaultPropertiesFile));
      if (resources.size() > 0) {
        if (resources.size() > 1) {
          LOG.warn("File '{}' found several times in the classloader : {}. Only the first one will be taken in account.",
              bundleDefaultPropertiesFile, classloader);
        }

        URL propertiesUrl = resources.get(0);
        if ( !alreadyLoadedResources.contains(propertiesUrl.toURI())) {
          LOG.debug("Found the ResourceBundle base file : {} from classloader : {}", propertiesUrl, classloader);
          LOG.debug("Add bundleName : {} from classloader : {}", bundleBaseName, classloader);
          bundleClassLoader.addResource(bundleDefaultPropertiesFile, classloader);
          alreadyLoadedResources.add(propertiesUrl.toURI());

          Properties bundleContent = new Properties();
          InputStream input = null;
          try {
            input = propertiesUrl.openStream();
            bundleContent.load(input);
            Enumeration<String> keysToAdd = (Enumeration<String>) bundleContent.propertyNames();
            while (keysToAdd.hasMoreElements()) {
              String key = keysToAdd.nextElement();
              if (keys.containsKey(key)) {
                LOG.warn("DUPLICATE KEY : Key '{}' defined in bundle '{}' is already defined in bundle '{}'. It is ignored.", new Object[] {
                    key, bundleBaseName, keys.get(key) });
              } else {
                keys.put(key, bundleBaseName);
              }
            }
          } finally {
            IOUtils.closeQuietly(input);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Fail to load '" + bundleDefaultPropertiesFile + "' in classloader : " + classloader, e);
      throw new SonarException("Fail to load '" + bundleDefaultPropertiesFile + "' in classloader : " + classloader, e);
    }
  }

  public String message(final Locale locale, final String key, final String defaultText, final Object... objects) {
    String translatedMessage = defaultText;
    try {
      if (isKeyForRuleDescription(key)) {
        // Rule descriptions are in HTML files, not in regular bundles
        translatedMessage = findRuleDescription(locale, key, defaultText);
      } else {
        translatedMessage = findStandardMessage(locale, key, defaultText, objects);
      }
    } catch (MissingResourceException e) {
      LOG.warn(e.getMessage());
      if (translatedMessage == null) {
        // when no translation has been found, the key is returned
        return key;
      }
    } catch (Exception e) {
      LOG.error("Exception when retrieving I18n string: ", e);
      if (translatedMessage == null) {
        // when no translation has been found, the key is returned
        return key;
      }
    }
    return translatedMessage;
  }

  protected boolean isKeyForRuleDescription(String key) {
    return StringUtils.startsWith(key, "rule.") && StringUtils.endsWith(key, ".description");
  }

  protected String findRuleDescription(final Locale locale, final String ruleDescriptionKey, final String defaultText) throws IOException {
    String translation = defaultText;
    String ruleNameKey = ruleDescriptionKey.replace(".description", ".name");

    String bundleBaseName = keys.get(ruleNameKey);
    if (bundleBaseName == null) {
      handleMissingBundle(ruleNameKey, defaultText, bundleBaseName);
    } else {
      Locale localeToUse = defineLocaleToUse(locale);
      String htmlFilePath = computeHtmlFilePath(bundleBaseName, ruleDescriptionKey, localeToUse);
      ClassLoader classLoader = bundleClassLoader.getClassLoaderForBundle(bundleBaseName, localeToUse);
      InputStream stream = classLoader.getResourceAsStream(htmlFilePath);
      if (stream == null) {
        throw new MissingResourceException("MISSING RULE DESCRIPTION : file '" + htmlFilePath
            + "' not found in any bundle. Default value is returned.", bundleBaseName, ruleDescriptionKey);
      }
      translation = IOUtils.toString(stream, "UTF-8");
    }

    return translation;
  }

  protected Locale defineLocaleToUse(final Locale locale) {
    Locale localeToUse = locale;
    if ( !registeredLocales.contains(locale)) {
      localeToUse = Locale.ENGLISH;
    }
    return localeToUse;
  }

  protected String extractRuleName(String ruleDescriptionKey) {
    int firstDotIndex = ruleDescriptionKey.indexOf(".");
    int secondDotIndex = ruleDescriptionKey.indexOf(".", firstDotIndex + 1);
    int thirdDotIndex = ruleDescriptionKey.indexOf(".", secondDotIndex + 1);
    return ruleDescriptionKey.substring(secondDotIndex + 1, thirdDotIndex);
  }

  protected String computeHtmlFilePath(String bundleBaseName, String ruleDescriptionKey, Locale locale) {
    String ruleName = extractRuleName(ruleDescriptionKey);
    if (Locale.ENGLISH.equals(locale)) {
      return bundleBaseName + "/" + ruleName + ".html";
    } else {
      return bundleBaseName + "_" + locale.toString() + "/" + ruleName + ".html";
    }
  }

  protected String findStandardMessage(final Locale locale, final String key, final String defaultText, final Object... objects) {
    String translation = defaultText;

    String bundleBaseName = keys.get(key);
    if (bundleBaseName == null) {
      handleMissingBundle(key, defaultText, bundleBaseName);
    } else {
      try {
        ResourceBundle bundle = ResourceBundle.getBundle(bundleBaseName, locale, bundleClassLoader);

        String value = bundle.getString(key);
        if ("".equals(value)) {
          if (translation == null) {
            throw new MissingResourceException("VOID KEY : Key '" + key + "' (from bundle '" + bundleBaseName + "') returns a void value.",
                bundleBaseName, key);
          }
          LOG.warn("VOID KEY : Key '{}' (from bundle '{}') returns a void value. Default value '{}' is returned.", new Object[] { key,
              bundleBaseName, defaultText });
        } else {
          translation = value;
        }
      } catch (MissingResourceException e) {
        if (translation == null) {
          throw e;
        }
        LOG.warn("BUNDLE NOT LOADED : Failed loading bundle {} from classloader {}. Default value '{}' is returned.", new Object[] {
            bundleBaseName, bundleClassLoader, defaultText });
      }
    }

    if (objects.length > 0) {
      LOG.debug("Translation : {}, {}, {}, {}", new String[] { locale.toString(), key, defaultText, Arrays.deepToString(objects) });
      return MessageFormat.format(translation, objects);
    } else {
      return translation;
    }
  }

  protected void handleMissingBundle(final String key, final String defaultText, String bundleBaseName) {
    if (defaultText == null) {
      throw new MissingResourceException("UNKNOWN KEY : Key '" + key
          + "' not found in any bundle, and no default value provided. The key is returned.", bundleBaseName, key);
    }
    LOG.warn("UNKNOWN KEY : Key '{}' not found in any bundle. Default value '{}' is returned.", key, defaultText);
    unknownKeys.put(key, defaultText);
  }

  /**
   * @return the unknownKeys
   */
  public Properties getUnknownKeys() {
    return unknownKeys;
  }

  private static class BundleClassLoader extends URLClassLoader {

    private Map<String, ClassLoader> resources = Maps.newHashMap();

    public BundleClassLoader() {
      super(new URL[] {}, null);
    }

    public void addResource(String resourceName, ClassLoader classloader) {
      resources.put(resourceName, classloader);
    }

    public ClassLoader getClassLoaderForBundle(String bundleBaseName, Locale locale) {
      StringBuilder resourceName = new StringBuilder(bundleBaseName);
      if (locale != null && !locale.equals(Locale.ENGLISH)) {
        resourceName.append("_");
        resourceName.append(locale);
      }
      resourceName.append(".properties");
      return resources.get(resourceName.toString());
    }

    @Override
    public URL findResource(String name) {
      if (resources.containsKey(name)) {
        return resources.get(name).getResource(name);
      }
      return null;
    }
  }
}
