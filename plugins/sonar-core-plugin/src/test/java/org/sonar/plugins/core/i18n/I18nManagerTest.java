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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.LanguagePack;
import org.sonar.api.platform.PluginRepository;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class I18nManagerTest {

  public static String ENGLISH_PACK_CLASS_NAME = "org.sonar.plugins.core.i18n.EnglishLanguagePack";
  public static String FRENCH_PACK_CLASS_NAME = "org.sonar.plugins.core.i18n.FrenchLanguagePack";
  public static String QUEBEC_PACK_CLASS_NAME = "org.sonar.plugins.core.i18n.QuebecLanguagePack";

  private static URL classSource = I18nManagerTest.class.getProtectionDomain().getCodeSource().getLocation();
  private I18nManager manager;

  @Before
  public void createManager() throws Exception {
    List<InstalledPlugin> plugins = Lists.newArrayList(new InstalledPlugin("test", getClass().getClassLoader()), new InstalledPlugin(
        "fake1", getClass().getClassLoader()), new InstalledPlugin("fake2", getClass().getClassLoader()));

    TestClassLoader englishPackClassLoader = new TestClassLoader(getClass().getClassLoader().getResource("I18n/EnglishPlugin.jar"));
    LanguagePack englishPack = (LanguagePack) englishPackClassLoader.loadClass(ENGLISH_PACK_CLASS_NAME).newInstance();

    TestClassLoader frenchPackClassLoader = new TestClassLoader(getClass().getClassLoader().getResource("I18n/FrenchPlugin.jar"));
    LanguagePack frenchPack = (LanguagePack) frenchPackClassLoader.loadClass(FRENCH_PACK_CLASS_NAME).newInstance();

    TestClassLoader quebecPackClassLoader = new TestClassLoader(getClass().getClassLoader().getResource("I18n/QuebecPlugin.jar"));
    LanguagePack quebecPack = (LanguagePack) quebecPackClassLoader.loadClass(QUEBEC_PACK_CLASS_NAME).newInstance();

    manager = new I18nManager(mock(PluginRepository.class), new LanguagePack[]{frenchPack, quebecPack, englishPack});
    manager.doStart(plugins);
  }

  @Test
  public void shouldTranslateWithoutRegionalVariant() {
    List<String> sentence = Arrays.asList("it", "is", "cold");
    String result = "";
    for (String token : sentence) {
      result += manager.message(Locale.FRENCH, token, token) + " ";
    }
    assertEquals("Il fait froid ", result);
  }

  @Test
  public void shouldTranslateWithRegionalVariant() {
    // it & is are taken from the french language pack
    // and cold is taken from the quebec language pack
    List<String> sentence = Arrays.asList("it", "is", "cold");
    String result = "";
    for (String token : sentence) {
      result += manager.message(Locale.CANADA_FRENCH, token, token) + " ";
    }
    assertEquals("Il fait frette ", result);
  }

  @Test
  public void shouldTranslateReturnsDefaultBundleValue() {
    String result = manager.message(Locale.FRENCH, "only.english", "Default");
    assertEquals("Ketchup", result);
  }

  @Test
  public void shouldTranslateUnknownValue() {
    String result = manager.message(Locale.FRENCH, "unknown", "Default value for Unknown");
    assertEquals("Default value for Unknown", result);
  }

  @Test
  public void shouldReturnKeyIfTranslationMissingAndNotDefaultProvided() throws Exception {
    String result = manager.message(Locale.ENGLISH, "unknown.key", null);
    assertEquals("unknown.key", result);
  }

  @Test
  public void testIsKeyForRuleDescription() throws Exception {
    assertTrue(manager.isKeyForRuleDescription("rule.squid.ArchitecturalConstraint.description"));
    assertFalse(manager.isKeyForRuleDescription("rule.squid.ArchitecturalConstraint.name"));
    assertFalse(manager.isKeyForRuleDescription("another.key"));
  }

  @Test
  public void testDefineLocaleToUse() throws Exception {
    assertThat(manager.defineLocaleToUse(Locale.CANADA_FRENCH), is(Locale.CANADA_FRENCH));
    // Locale not supported => get the English one
    assertThat(manager.defineLocaleToUse(Locale.JAPAN), is(Locale.ENGLISH));
  }

  @Test
  public void testExtractRuleName() throws Exception {
    assertThat(manager.extractRuleName("rule.squid.ArchitecturalConstraint.description"), is("ArchitecturalConstraint"));
  }

  @Test
  public void testComputeHtmlFilePath() throws Exception {
    assertThat(manager.computeHtmlFilePath("org/sonar/i18n/test", "rule.test.fakerule.description", Locale.FRENCH),
        is("org/sonar/i18n/test_fr/fakerule.html"));
    assertThat(manager.computeHtmlFilePath("org/sonar/i18n/test", "rule.test.fakerule.description", Locale.ENGLISH),
        is("org/sonar/i18n/test/fakerule.html"));
  }

  @Test
  public void shouldReturnRuleDescriptionFromHTMLFile() throws Exception {
    String result = manager.message(Locale.FRENCH, "rule.test.fakerule.description", "foo");
    assertThat(result, is("<h1>Règle bidon</h1>\nC'est la description de la règle bidon."));
    // Locale not supported => get the English translation
    result = manager.message(Locale.JAPAN, "rule.test.fakerule.description", "foo");
    assertThat(result, is("<h1>Fake Rule</h1>\nThis is the description of the fake rule."));
  }

  @Test
  public void shouldReturnEnglishRuleDescriptionFromMissingHTMLFileInFrench() throws Exception {
    String result = manager.message(Locale.FRENCH, "rule.test.anotherfakerule.description", "foo");
    assertThat(result, is("<h1>Another Fake Rule</h1>\nThis is the description of the fake rule."));

  }

  public static class TestClassLoader extends URLClassLoader {

    public TestClassLoader(URL url) {
      super(new URL[]{url, classSource}, Thread.currentThread().getContextClassLoader());
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class c = findLoadedClass(name);
      if (c == null) {
        if (name.equals(ENGLISH_PACK_CLASS_NAME) || name.equals(QUEBEC_PACK_CLASS_NAME) || name.equals(FRENCH_PACK_CLASS_NAME)) {
          c = findClass(name);
        } else {
          return super.loadClass(name, resolve);
        }
      }
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }
  }

}
