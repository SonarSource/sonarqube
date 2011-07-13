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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.LanguagePack;
import org.sonar.api.platform.PluginRepository;

import com.google.common.collect.Lists;

public class I18nManagerTest {

  public static String TEST_PLUGIN_CLASS_NAME = "org.sonar.plugins.core.i18n.StandardPlugin";
  public static String FRENCH_PACK_CLASS_NAME = "org.sonar.plugins.core.i18n.FrenchLanguagePack";
  public static String QUEBEC_PACK_CLASS_NAME = "org.sonar.plugins.core.i18n.QuebecLanguagePack";

  private static URL classSource = I18nManagerTest.class.getProtectionDomain().getCodeSource().getLocation();
  private I18nManager manager;

  @Before
  public void createManager() throws Exception {
    List<InstalledPlugin> plugins = Lists.newArrayList(new InstalledPlugin("test", new TestClassLoader(getClass().getClassLoader()
        .getResource("StandardPlugin.jar"))), new InstalledPlugin("fake1", getClass().getClassLoader()), new InstalledPlugin("fake2",
        getClass().getClassLoader()));

    TestClassLoader frenchPackClassLoader = new TestClassLoader(getClass().getClassLoader().getResource("FrenchPlugin.jar"));
    LanguagePack frenchPack = (LanguagePack) frenchPackClassLoader.loadClass(FRENCH_PACK_CLASS_NAME).newInstance();

    TestClassLoader quebecPackClassLoader = new TestClassLoader(getClass().getClassLoader().getResource("QuebecPlugin.jar"));
    LanguagePack quebecPack = (LanguagePack) quebecPackClassLoader.loadClass(QUEBEC_PACK_CLASS_NAME).newInstance();

    manager = new I18nManager(mock(PluginRepository.class), new LanguagePack[] { frenchPack, quebecPack });
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
    Assert.assertEquals(1, manager.getUnknownKeys().size());
    Assert.assertEquals("Default value for Unknown", manager.getUnknownKeys().getProperty("unknown"));
  }

  @Test
  public void shouldReturnKeyIfTranslationMissingAndNotDefaultProvided() throws Exception {
    String result = manager.message(Locale.ENGLISH, "unknown.key", null);
    assertEquals("unknown.key", result);
    Assert.assertEquals(0, manager.getUnknownKeys().size());
  }

  public static class TestClassLoader extends URLClassLoader {

    public TestClassLoader(URL url) {
      super(new URL[] { url, classSource }, Thread.currentThread().getContextClassLoader());
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class c = findLoadedClass(name);
      if (c == null) {
        if (name.equals(TEST_PLUGIN_CLASS_NAME) || name.equals(QUEBEC_PACK_CLASS_NAME) || name.equals(FRENCH_PACK_CLASS_NAME)) {
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
