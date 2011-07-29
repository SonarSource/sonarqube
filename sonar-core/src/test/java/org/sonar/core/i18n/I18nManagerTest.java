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
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.sonar.core.i18n.I18nManager.BUNDLE_PACKAGE;

public class I18nManagerTest {
  private I18nManager manager;
  private ClassLoader coreClassLoader;
  private ClassLoader sqaleClassLoader;

  @Before
  public void init() {
    coreClassLoader = newCoreClassLoader();
    sqaleClassLoader = newSqaleClassLoader();
    Map<String, ClassLoader> bundleToClassLoaders = Maps.newHashMap();
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "core", coreClassLoader);
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "checkstyle", coreClassLoader);
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "sqale", sqaleClassLoader);
    manager = new I18nManager(bundleToClassLoaders);
    manager.start();
  }

  @Test
  public void shouldExtractPluginFromKey() {
    Map<String, ClassLoader> bundleToClassLoaders = Maps.newHashMap();
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "core", getClass().getClassLoader());
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "checkstyle", getClass().getClassLoader());
    bundleToClassLoaders.put(BUNDLE_PACKAGE + "sqale", getClass().getClassLoader());
    I18nManager i18n = new I18nManager(bundleToClassLoaders);
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
    assertThat(manager.getClassLoaderForProperty("foo.unknown"), nullValue());
    assertThat(manager.getClassLoaderForProperty("by"), Is.is(coreClassLoader));
    assertThat(manager.getClassLoaderForProperty("sqale.page"), Is.is(sqaleClassLoader));
  }

  @Test
  public void shouldFindEnglishFile() {
    String html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name" /* any property in the same bundle */);
    assertThat(html, Is.is("This is the architecture rule"));
  }

  @Test
  public void shouldNotFindFile() {
    String html = manager.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "checkstyle.rule1.name" /* any property in the same bundle */);
    assertThat(html, nullValue());
  }

  @Test
  public void shouldFindFrenchFile() {
    String html = manager.messageFromFile(Locale.FRENCH, "ArchitectureRule.html", "checkstyle.rule1.name" /* any property in the same bundle */);
    assertThat(html, Is.is("Règle d'architecture"));
  }

  @Test
  public void shouldNotFindMissingLocale() {
    String html = manager.messageFromFile(Locale.CHINA, "ArchitectureRule.html", "checkstyle.rule1.name" /* any property in the same bundle */);
    assertThat(html, nullValue());
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
