/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.sonar.core.i18n.I18nManager.BUNDLE_PACKAGE;

public class I18nManagerTest {

  private static Locale defaultLocale;
  private I18nManager manager;
  private ClassLoader coreClassLoader;
  private ClassLoader sqaleClassLoader;
  private ClassLoader forgeClassLoader;

  /**
   * See http://jira.codehaus.org/browse/SONAR-2927
   */
  @BeforeClass
  public static void fixDefaultLocaleBug() {
    defaultLocale = Locale.getDefault();
    Locale.setDefault(Locale.ENGLISH);
  }

  @AfterClass
  public static void revertFix() {
    Locale.setDefault(defaultLocale);
  }

  @Before
  public void init() {
    Map<String, ClassLoader> bundleToClassLoaders = Maps.newHashMap();
    // following represents the English language pack + a core plugin : they use the same classloader
    coreClassLoader = newCoreClassLoader();
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "core", coreClassLoader);
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "checkstyle", coreClassLoader);
    // following represents a commercial plugin that must embed all its bundles, whatever the language
    sqaleClassLoader = newSqaleClassLoader();
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "sqale", sqaleClassLoader);
    // following represents a forge plugin that embeds only the english bundle, and lets the language
    // packs embed all the bundles for the other languages
    forgeClassLoader = newForgeClassLoader();
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "forge", forgeClassLoader);

    manager = new I18nManager(bundleToClassLoaders, coreClassLoader);
    manager.start();
  }

  @Test
  public void shouldExtractPluginFromKey() {
    Map<String, ClassLoader> bundleToClassLoaders = Maps.newHashMap();
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "core", getClass().getClassLoader());
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "checkstyle", getClass().getClassLoader());
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "sqale", getClass().getClassLoader());
    I18nManager i18n = new I18nManager(bundleToClassLoaders, coreClassLoader);
    i18n.start();

    assertThat(i18n.extractBundleFromKey("by"), Is.is(BUNDLE_PACKAGE + "core"));
    assertThat(i18n.extractBundleFromKey("violations_drilldown.page"), Is.is(BUNDLE_PACKAGE + "core"));
    assertThat(i18n.extractBundleFromKey("checkstyle.rule1.name"), Is.is(BUNDLE_PACKAGE + "checkstyle"));
    assertThat(i18n.extractBundleFromKey("sqale.console.page"), Is.is(BUNDLE_PACKAGE + "sqale"));
  }

  @Test
  public void shouldFindKeysInEnglishLanguagePack() {
    assertThat(manager.message(Locale.ENGLISH, "checkstyle.rule1.name", null), Is.is("Rule one"));
    assertThat(manager.message(Locale.ENGLISH, "by", null), Is.is("By"));
    assertThat(manager.message(Locale.ENGLISH, "sqale.page", null), Is.is("Sqale page title"));

    assertThat(manager.message(Locale.FRENCH, "checkstyle.rule1.name", null), Is.is("Rule un"));
    assertThat(manager.message(Locale.FRENCH, "by", null), Is.is("Par"));
    assertThat(manager.message(Locale.FRENCH, "sqale.page", null), Is.is("Titre de la page Sqale"));
  }

  @Test
  public void shouldUseDefaultLocale() {
    assertThat(manager.message(Locale.CHINA, "checkstyle.rule1.name", null), Is.is("Rule one"));
    assertThat(manager.message(Locale.CHINA, "by", null), Is.is("By"));
    assertThat(manager.message(Locale.CHINA, "sqale.page", null), Is.is("Sqale page title"));
  }

  @Test
  public void shouldUseLanguagePack() {
    assertThat(manager.message(Locale.FRENCH, "checkstyle.rule1.name", null), Is.is("Rule un"));
    assertThat(manager.message(Locale.FRENCH, "by", null), Is.is("Par"));
    assertThat(manager.message(Locale.FRENCH, "sqale.page", null), Is.is("Titre de la page Sqale"));
  }

  @Test
  public void shouldReturnDefaultValueIfMissingKey() {
    assertThat(manager.message(Locale.ENGLISH, "foo.unknown", "default"), Is.is("default"));
    assertThat(manager.message(Locale.FRENCH, "foo.unknown", "default"), Is.is("default"));
  }

  @Test
  public void shouldAcceptEmptyLabels() {
    assertThat(manager.message(Locale.ENGLISH, "empty", "default"), Is.is(""));
    assertThat(manager.message(Locale.FRENCH, "empty", "default"), Is.is(""));
  }

  @Test
  public void shouldFormatMessageWithParameters() {
    assertThat(manager.message(Locale.ENGLISH, "with.parameters", null, "one", "two"), Is.is("First is one and second is two"));
  }

  @Test
  public void shouldUseDefaultLocaleIfMissingValueInLocalizedBundle() {
    assertThat(manager.message(Locale.FRENCH, "only.in.english", null), Is.is("Missing in French bundle"));
    assertThat(manager.message(Locale.CHINA, "only.in.english", null), Is.is("Missing in French bundle"));
  }

  @Test
  public void shouldGetClassLoaderByProperty() {
    assertThat(manager.getClassLoaderForProperty("foo.unknown", Locale.ENGLISH), nullValue());
    assertThat(manager.getClassLoaderForProperty("by", Locale.ENGLISH), Is.is(coreClassLoader));
    // The following plugin defines its own bundles, whatever the language
    assertThat(manager.getClassLoaderForProperty("sqale.page", Locale.ENGLISH), Is.is(sqaleClassLoader));
    assertThat(manager.getClassLoaderForProperty("sqale.page", Locale.FRENCH), Is.is(sqaleClassLoader));
    // The following plugin defines only the English bundle, and lets the language packs handle the translations
    assertThat(manager.getClassLoaderForProperty("forge_plugin.page", Locale.ENGLISH), Is.is(forgeClassLoader));
    assertThat(manager.getClassLoaderForProperty("forge_plugin.page", Locale.FRENCH), Is.is(coreClassLoader));
  }

  @Test
  public void shouldFindEnglishFile() {
    String html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name" /*
                                                                                                            * any property in the same
                                                                                                            * bundle
                                                                                                            */, false);
    assertThat(html, Is.is("This is the architecture rule"));
  }

  @Test
  public void shouldNotFindFile() {
    String html = manager.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "checkstyle.rule1.name" /* any property in the same bundle */, false);
    assertThat(html, nullValue());
  }

  @Test
  public void shouldFindFrenchFile() {
    String html = manager.messageFromFile(Locale.FRENCH, "ArchitectureRule.html", "checkstyle.rule1.name" /* any property in the same bundle */, false);
    assertThat(html, Is.is("Règle d'architecture"));
  }

  @Test
  public void shouldNotFindMissingLocale() {
    String html = manager.messageFromFile(Locale.CHINA, "ArchitectureRule.html", "checkstyle.rule1.name" /* any property in the same bundle */, false);
    assertThat(html, nullValue());
  }

  @Test
  public void shouldNotKeepInCache() {
    assertThat(manager.getFileContentCache().size(), Is.is(0));
    boolean keepInCache = false;
    String html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name" /*
                                                                                                            * any property in the same
                                                                                                            * bundle
                                                                                                            */, keepInCache);

    assertThat(html, not(nullValue()));
    assertThat(manager.getFileContentCache().size(), Is.is(0));
  }

  @Test
  public void shouldKeepInCache() {
    assertThat(manager.getFileContentCache().size(), Is.is(0));
    boolean keepInCache = true;
    String html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name" /*
                                                                                                            * any property in the same
                                                                                                            * bundle
                                                                                                            */, keepInCache);

    assertThat(html, not(nullValue()));
    Map<String, Map<Locale, String>> cache = manager.getFileContentCache();
    assertThat(cache.size(), Is.is(1));
    assertThat(cache.get("ArchitectureRule.html").get(Locale.ENGLISH), Is.is("This is the architecture rule"));
  }

  // see SONAR-3596
  @Test
  public void shouldLookInCoreClassloaderForPluginsThatDontEmbedAllLanguages() {
    assertThat(manager.message(Locale.ENGLISH, "forge_plugin.page", null)).isEqualTo("This is my plugin");
    assertThat(manager.message(Locale.FRENCH, "forge_plugin.page", null)).isEqualTo("Mon plugin!");
  }

  // see SONAR-3783 => test that there will be no future regression on fallback for keys spread accross several classloaders
  @Test
  public void shouldFallbackOnOriginalPluginIfTranslationNotPresentInLanguagePack() {
    // the "forge_plugin.page" has been translated in French
    assertThat(manager.message(Locale.FRENCH, "forge_plugin.page", null)).isEqualTo("Mon plugin!");
    // but not the "forge_plugin.key_not_translated" key
    assertThat(manager.message(Locale.FRENCH, "forge_plugin.key_not_translated", null)).isEqualTo("Key Not Translated");
  }

  private URLClassLoader newForgeClassLoader() {
    return newClassLoader("/org/sonar/core/i18n/forgePlugin/");
  }

  private URLClassLoader newSqaleClassLoader() {
    return newClassLoader("/org/sonar/core/i18n/sqalePlugin/");
  }

  private URLClassLoader newCoreClassLoader() {
    return newClassLoader("/org/sonar/core/i18n/englishPack/", "/org/sonar/core/i18n/frenchPack/");
  }

  private URLClassLoader newClassLoader(String... resourcePaths) {
    URL[] urls = new URL[resourcePaths.length];
    for (int index = 0; index < resourcePaths.length; index++) {
      urls[index] = getClass().getResource(resourcePaths[index]);
    }
    return new URLClassLoader(urls);
  }
}
