/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.l18n;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.extension.CoreExtension;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerI18nTest {

  private TestSystem2 system2 = new TestSystem2();
  private ServerI18n underTest;

  @Before
  public void before() {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    List<PluginInfo> plugins = singletonList(newPlugin("checkstyle"));
    when(pluginRepository.getPluginInfos()).thenReturn(plugins);

    CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);
    Stream<CoreExtension> coreExtensions = Stream.of(newCoreExtension("coreext"), newCoreExtension("othercorext"));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(coreExtensions);

    underTest = new ServerI18n(pluginRepository, system2, coreExtensionRepository);
    underTest.doStart(getClass().getClassLoader());
  }

  @Test
  public void get_english_labels() {
    assertThat(underTest.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");
    assertThat(underTest.message(Locale.ENGLISH, "coreext.rule1.name", null)).isEqualTo("Rule one");
  }

  @Test
  public void get_english_labels_when_default_locale_is_not_english() {
    Locale defaultLocale = Locale.getDefault();
    try {
      Locale.setDefault(Locale.FRENCH);
      assertThat(underTest.message(Locale.ENGLISH, "any", null)).isEqualTo("Any");
      assertThat(underTest.message(Locale.ENGLISH, "coreext.rule1.name", null)).isEqualTo("Rule one");
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  @Test
  public void get_labels_from_french_pack() {
    assertThat(underTest.message(Locale.FRENCH, "coreext.rule1.name", null)).isEqualTo("Rule un");
    assertThat(underTest.message(Locale.FRENCH, "any", null)).isEqualTo("Tous");
  }

  private static PluginInfo newPlugin(String key) {
    PluginInfo plugin = mock(PluginInfo.class);
    when(plugin.getKey()).thenReturn(key);
    return plugin;
  }

  private static CoreExtension newCoreExtension(String name) {
    CoreExtension res = mock(CoreExtension.class);
    when(res.getName()).thenReturn(name);
    return res;
  }

}
