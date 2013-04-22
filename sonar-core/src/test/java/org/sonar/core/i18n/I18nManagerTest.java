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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class I18nManagerTest {

  private static Locale defaultLocale;
  private I18nManager manager;

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
    PluginRepository pluginRepository = mock(PluginRepository.class);
    List<PluginMetadata> plugins = Arrays.asList(newPlugin("sqale"), newPlugin("frpack"), newPlugin("core"), newPlugin("checkstyle"), newPlugin("other"));
    when(pluginRepository.getMetadata()).thenReturn(plugins);

    I18nClassloader i18nClassloader = new I18nClassloader(new ClassLoader[] {
      newCoreClassloader(), newFrenchPackClassloader(), newSqaleClassloader(), newCheckstyleClassloader()
    });
    manager = new I18nManager(pluginRepository);
    manager.doStart(i18nClassloader);
  }

  @Test
  public void should_introspect_all_available_properties() {
    assertThat(manager.getPropertyKeys().contains("by")).isTrue();
    assertThat(manager.getPropertyKeys().contains("only.in.english")).isTrue();
    assertThat(manager.getPropertyKeys().contains("sqale.page")).isTrue();
    assertThat(manager.getPropertyKeys().contains("unknown")).isFalse();
  }

  @Test
  public void should_get_english_labels() {
    assertThat(manager.message(Locale.ENGLISH, "by", null)).isEqualTo("By");
    assertThat(manager.message(Locale.ENGLISH, "sqale.page", null)).isEqualTo("Sqale page title");
    assertThat(manager.message(Locale.ENGLISH, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
  }

  @Test
  public void should_get_labels_from_french_pack() {
    assertThat(manager.message(Locale.FRENCH, "checkstyle.rule1.name", null)).isEqualTo("Rule un");
    assertThat(manager.message(Locale.FRENCH, "by", null)).isEqualTo("Par");

    // language pack
    assertThat(manager.message(Locale.FRENCH, "sqale.page", null)).isEqualTo("Titre de la page Sqale");
  }

  @Test
  public void should_get_french_label_if_swiss_country() {
    Locale swiss = new Locale("fr", "CH");
    assertThat(manager.message(swiss, "checkstyle.rule1.name", null)).isEqualTo("Rule un");
    assertThat(manager.message(swiss, "by", null)).isEqualTo("Par");

    // language pack
    assertThat(manager.message(swiss, "sqale.page", null)).isEqualTo("Titre de la page Sqale");
  }

  @Test
  public void should_fallback_to_default_locale() {
    assertThat(manager.message(Locale.CHINA, "checkstyle.rule1.name", null)).isEqualTo("Rule one");
    assertThat(manager.message(Locale.CHINA, "by", null)).isEqualTo("By");
    assertThat(manager.message(Locale.CHINA, "sqale.page", null)).isEqualTo("Sqale page title");
  }

  @Test
  public void should_return_default_value_if_missing_key() {
    assertThat(manager.message(Locale.ENGLISH, "unknown", "default")).isEqualTo("default");
    assertThat(manager.message(Locale.FRENCH, "unknown", "default")).isEqualTo("default");
  }

  @Test
  public void should_accept_empty_labels() {
    assertThat(manager.message(Locale.ENGLISH, "empty", "default")).isEqualTo("");
    assertThat(manager.message(Locale.FRENCH, "empty", "default")).isEqualTo("");
  }

  @Test
  public void shouldFormatMessageWithParameters() {
    assertThat(manager.message(Locale.ENGLISH, "with.parameters", null, "one", "two")).isEqualTo("First is one and second is two");
  }

  @Test
  public void shouldUseDefaultLocaleIfMissingValueInLocalizedBundle() {
    assertThat(manager.message(Locale.FRENCH, "only.in.english", null)).isEqualTo("Missing in French bundle");
    assertThat(manager.message(Locale.CHINA, "only.in.english", null)).isEqualTo("Missing in French bundle");
  }

  @Test
  public void should_locate_english_file() {
    String html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name", false);
    assertThat(html).isEqualTo("This is the architecture rule");
  }

  @Test
  public void should_return_null_if_file_not_found() {
    String html = manager.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "checkstyle.rule1.name", false);
    assertThat(html).isNull();
  }

  @Test
  public void should_return_null_if_rule_not_internationalized() {
    String html = manager.messageFromFile(Locale.ENGLISH, "UnknownRule.html", "foo.rule1.name", false);
    assertThat(html).isNull();
  }

  @Test
  public void should_locate_french_file() {
    String html = manager.messageFromFile(Locale.FRENCH, "ArchitectureRule.html", "checkstyle.rule1.name", false);
    assertThat(html).isEqualTo("Règle d'architecture");
  }

  @Test
  public void should_locate_file_with_missing_locale() {
    String html = manager.messageFromFile(Locale.CHINA, "ArchitectureRule.html", "checkstyle.rule1.name", false);
    assertThat(html).isNull();
  }

  @Test
  public void should_not_keep_in_cache() {
    assertThat(manager.getFileContentCache()).isEmpty();
    boolean keepInCache = false;
    String html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name", keepInCache);

    assertThat(html).isNotNull();
    assertThat(manager.getFileContentCache()).isEmpty();
  }

  @Test
  public void should_keep_in_cache() {
    assertThat(manager.getFileContentCache()).isEmpty();
    boolean keepInCache = true;
    String html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name", keepInCache);
    assertThat(html).isEqualTo("This is the architecture rule");

    html = manager.messageFromFile(Locale.ENGLISH, "ArchitectureRule.html", "checkstyle.rule1.name", keepInCache);
    assertThat(html).isEqualTo("This is the architecture rule");
    assertThat(manager.getFileContentCache()).hasSize(1);

    html = manager.messageFromFile(Locale.FRENCH, "ArchitectureRule.html", "checkstyle.rule1.name", keepInCache);
    assertThat(html).isEqualTo("Règle d'architecture");
    assertThat(manager.getFileContentCache()).hasSize(1);
  }

  static URLClassLoader newCoreClassloader() {
    return newClassLoader("/org/sonar/core/i18n/corePlugin/");
  }

  static URLClassLoader newCheckstyleClassloader() {
    return newClassLoader("/org/sonar/core/i18n/checkstylePlugin/");
  }

  /**
   * Example of plugin that embeds its own translations (English + French).
   */
  static URLClassLoader newSqaleClassloader() {
    return newClassLoader("/org/sonar/core/i18n/sqalePlugin/");
  }

  /**
   * "Language Pack" contains various translations for different plugins.
   */
  static URLClassLoader newFrenchPackClassloader() {
    return newClassLoader("/org/sonar/core/i18n/frenchPack/");
  }

  private static URLClassLoader newClassLoader(String... resourcePaths) {
    URL[] urls = new URL[resourcePaths.length];
    for (int index = 0; index < resourcePaths.length; index++) {
      urls[index] = I18nManagerTest.class.getResource(resourcePaths[index]);
    }
    return new URLClassLoader(urls);
  }

  private PluginMetadata newPlugin(String key) {
    PluginMetadata plugin = mock(PluginMetadata.class);
    when(plugin.getKey()).thenReturn(key);
    return plugin;
  }
}
