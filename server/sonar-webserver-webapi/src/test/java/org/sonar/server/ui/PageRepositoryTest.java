/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.ui;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.Page.Qualifier;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.api.web.page.Page.Scope.GLOBAL;

public class PageRepositoryTest {

  @Rule
  public LogTester logTester = new LogTester();

  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private final CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);

  private PageRepository underTest = new PageRepository(pluginRepository, coreExtensionRepository);

  @Before
  public void setUp() {
    when(pluginRepository.hasPlugin(any())).thenReturn(true);
    when(pluginRepository.getPluginInfo(any())).thenReturn(new PluginInfo("unused"));
  }

  @Test
  public void pages_from_different_page_definitions_ordered_by_key() {
    PageDefinition firstPlugin = context -> context
      .addPage(Page.builder("my_plugin/K1").setName("N1").build())
      .addPage(Page.builder("my_plugin/K3").setName("N3").build());
    PageDefinition secondPlugin = context -> context.addPage(Page.builder("my_plugin/K2").setName("N2").build());
    underTest = new PageRepository(pluginRepository, coreExtensionRepository, new PageDefinition[]{firstPlugin, secondPlugin});
    underTest.start();

    List<Page> result = underTest.getAllPages();

    assertThat(result)
      .extracting(Page::getKey, Page::getName)
      .containsExactly(
        tuple("my_plugin/K1", "N1"),
        tuple("my_plugin/K2", "N2"),
        tuple("my_plugin/K3", "N3"));
  }

  @Test
  public void filter_by_navigation_and_qualifier() {
    PageDefinition plugin = context -> context
      // Default with GLOBAL navigation and no qualifiers
      .addPage(Page.builder("my_plugin/K1").setName("K1").build())
      .addPage(Page.builder("my_plugin/K2").setName("K2").setScope(COMPONENT).setComponentQualifiers(Qualifier.PROJECT).build())
      .addPage(Page.builder("my_plugin/K3").setName("K3").setScope(COMPONENT).setComponentQualifiers(Qualifier.MODULE).build())
      .addPage(Page.builder("my_plugin/K4").setName("K4").setScope(GLOBAL).build())
      .addPage(Page.builder("my_plugin/K5").setName("K5").setScope(COMPONENT).setComponentQualifiers(Qualifier.VIEW).build())
      .addPage(Page.builder("my_plugin/K6").setName("K6").setScope(COMPONENT).setComponentQualifiers(Qualifier.APP).build());
    underTest = new PageRepository(pluginRepository, coreExtensionRepository, new PageDefinition[]{plugin});
    underTest.start();

    List<Page> result = underTest.getComponentPages(false, ComponentQualifiers.PROJECT);

    assertThat(result)
      .extracting(Page::getKey)
      .containsExactly("my_plugin/K2");
  }

  @Test
  public void empty_pages_if_no_page_definition() {
    underTest.start();

    List<Page> result = underTest.getAllPages();

    assertThat(result).isEmpty();
  }

  @Test
  public void filter_pages_without_qualifier() {
    PageDefinition plugin = context -> context
      .addPage(Page.builder("my_plugin/K1").setName("N1").build())
      .addPage(Page.builder("my_plugin/K2").setName("N2").build())
      .addPage(Page.builder("my_plugin/K3").setName("N3").build());
    underTest = new PageRepository(pluginRepository, coreExtensionRepository, new PageDefinition[]{plugin});
    underTest.start();

    List<Page> result = underTest.getGlobalPages(false);

    assertThat(result)
      .extracting(Page::getKey)
      .containsExactly("my_plugin/K1", "my_plugin/K2", "my_plugin/K3");
  }

  @Test
  public void fail_if_pages_called_before_server_startup() {
    assertThatThrownBy(() -> underTest.getAllPages())
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Pages haven't been initialized yet");
  }

  @Test
  public void fail_if_page_with_unknown_plugin() {
    PageDefinition governance = context -> context.addPage(Page.builder("governance/my_key").setName("N1").build());
    PageDefinition plugin42 = context -> context.addPage(Page.builder("plugin_42/my_key").setName("N2").build());
    pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.hasPlugin("governance")).thenReturn(true);
    underTest = new PageRepository(pluginRepository, coreExtensionRepository, new PageDefinition[]{governance, plugin42});

    assertThatThrownBy(() -> underTest.start())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Page 'N2' references plugin 'plugin_42' that does not exist");
  }
}
