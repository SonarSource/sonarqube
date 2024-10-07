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
package org.sonar.server.plugins.edition;

import java.util.Random;
import org.junit.Test;
import org.sonar.core.platform.PluginInfo;
import org.sonar.updatecenter.common.Plugin;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EditionBundledPluginsTest {

  private final Random random = new Random();

  @Test
  public void isEditionBundled_on_Plugin_fails_with_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> EditionBundledPlugins.isEditionBundled((Plugin) null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void isEditionBundled_on_Plugin_returns_false_for_SonarSource_and_non_commercial_license() {
    Plugin plugin = newPlugin(randomizeCase("SonarSource"), secure().nextAlphanumeric(3));

    assertThat(EditionBundledPlugins.isEditionBundled(plugin)).isFalse();
  }

  @Test
  public void isEditionBundled_on_Plugin_returns_false_for_license_SonarSource_and_non_SonarSource_organization() {
    Plugin plugin = newPlugin(secure().nextAlphanumeric(3), randomizeCase("SonarSource"));

    assertThat(EditionBundledPlugins.isEditionBundled(plugin)).isFalse();
  }

  @Test
  public void isEditionBundled_on_Plugin_returns_false_for_license_Commercial_and_non_SonarSource_organization() {
    Plugin plugin = newPlugin(secure().nextAlphanumeric(3), randomizeCase("Commercial"));

    assertThat(EditionBundledPlugins.isEditionBundled(plugin)).isFalse();
  }

  @Test
  public void isEditionBundled_on_Plugin_returns_true_for_organization_SonarSource_and_license_SonarSource_case_insensitive() {
    Plugin plugin = newPlugin(randomizeCase("SonarSource"), randomizeCase("SonarSource"));

    assertThat(EditionBundledPlugins.isEditionBundled(plugin)).isTrue();
  }

  @Test
  public void isEditionBundled_on_Plugin_returns_true_for_organization_SonarSource_and_license_Commercial_case_insensitive() {
    Plugin plugin = newPlugin(randomizeCase("SonarSource"), randomizeCase("Commercial"));

    assertThat(EditionBundledPlugins.isEditionBundled(plugin)).isTrue();
  }

  @Test
  public void isEditionBundled_on_PluginInfo_fails_with_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> EditionBundledPlugins.isEditionBundled((PluginInfo) null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void isEditionBundled_on_PluginInfo_returns_false_for_SonarSource_and_non_commercial_license() {
    PluginInfo pluginInfo = newPluginInfo(randomizeCase("SonarSource"), secure().nextAlphanumeric(3));

    assertThat(EditionBundledPlugins.isEditionBundled(pluginInfo)).isFalse();
  }

  @Test
  public void isEditionBundled_on_PluginInfo_returns_false_for_license_SonarSource_and_non_SonarSource_organization() {
    PluginInfo pluginInfo = newPluginInfo(secure().nextAlphanumeric(3), randomizeCase("SonarSource"));

    assertThat(EditionBundledPlugins.isEditionBundled(pluginInfo)).isFalse();
  }

  @Test
  public void isEditionBundled_on_PluginInfo_returns_false_for_license_Commercial_and_non_SonarSource_organization() {
    PluginInfo pluginInfo = newPluginInfo(secure().nextAlphanumeric(3), randomizeCase("Commercial"));

    assertThat(EditionBundledPlugins.isEditionBundled(pluginInfo)).isFalse();
  }

  @Test
  public void isEditionBundled_on_PluginInfo_returns_true_for_organization_SonarSource_and_license_SonarSource_case_insensitive() {
    PluginInfo pluginInfo = newPluginInfo(randomizeCase("SonarSource"), randomizeCase("SonarSource"));

    assertThat(EditionBundledPlugins.isEditionBundled(pluginInfo)).isTrue();
  }

  @Test
  public void isEditionBundled_on_PluginINfo_returns_true_for_organization_SonarSource_and_license_Commercial_case_insensitive() {
    PluginInfo pluginInfo = newPluginInfo(randomizeCase("SonarSource"), randomizeCase("Commercial"));

    assertThat(EditionBundledPlugins.isEditionBundled(pluginInfo)).isTrue();
  }

  private String randomizeCase(String s) {
    return s.chars()
      .map(c -> random.nextBoolean() ? Character.toUpperCase(c) : Character.toLowerCase(c))
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString();
  }

  private PluginInfo newPluginInfo(String organization, String license) {
    PluginInfo pluginInfo = new PluginInfo(secure().nextAlphanumeric(2));
    if (random.nextBoolean()) {
      pluginInfo.setName(secure().nextAlphanumeric(3));
    }
    if (random.nextBoolean()) {
      pluginInfo.setOrganizationUrl(secure().nextAlphanumeric(4));
    }
    if (random.nextBoolean()) {
      pluginInfo.setIssueTrackerUrl(secure().nextAlphanumeric(5));
    }
    if (random.nextBoolean()) {
      pluginInfo.setIssueTrackerUrl(secure().nextAlphanumeric(6));
    }
    if (random.nextBoolean()) {
      pluginInfo.setBasePlugin(secure().nextAlphanumeric(7));
    }
    if (random.nextBoolean()) {
      pluginInfo.setHomepageUrl(secure().nextAlphanumeric(8));
    }
    return pluginInfo
      .setOrganizationName(organization)
      .setLicense(license);
  }

  private Plugin newPlugin(String organization, String license) {
    Plugin plugin = Plugin.factory(secure().nextAlphanumeric(2));
    if (random.nextBoolean()) {
      plugin.setName(secure().nextAlphanumeric(3));
    }
    if (random.nextBoolean()) {
      plugin.setOrganizationUrl(secure().nextAlphanumeric(4));
    }
    if (random.nextBoolean()) {
      plugin.setTermsConditionsUrl(secure().nextAlphanumeric(5));
    }
    if (random.nextBoolean()) {
      plugin.setIssueTrackerUrl(secure().nextAlphanumeric(6));
    }
    if (random.nextBoolean()) {
      plugin.setCategory(secure().nextAlphanumeric(7));
    }
    if (random.nextBoolean()) {
      plugin.setHomepageUrl(secure().nextAlphanumeric(8));
    }
    return plugin
      .setLicense(license)
      .setOrganization(organization);
  }
}
