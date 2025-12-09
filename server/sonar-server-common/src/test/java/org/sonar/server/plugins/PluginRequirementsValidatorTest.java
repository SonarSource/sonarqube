/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.plugins;

import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginInfo.RequiredPlugin;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginRequirementsValidatorTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void unloadIncompatiblePlugins_removes_incompatible_plugins() {
    PluginInfo pluginE = new PluginInfo("pluginE");

    PluginInfo pluginD = new PluginInfo("pluginD")
      .setBasePlugin("pluginC");
    PluginInfo pluginC = new PluginInfo("pluginC")
      .setBasePlugin("pluginB");
    PluginInfo pluginB = new PluginInfo("pluginB")
      .addRequiredPlugin(RequiredPlugin.parse("pluginA:1.0"));
    Map<String, PluginInfo> plugins = new HashMap<>();
    plugins.put(pluginB.getKey(), pluginB);
    plugins.put(pluginC.getKey(), pluginC);
    plugins.put(pluginD.getKey(), pluginD);
    plugins.put(pluginE.getKey(), pluginE);

    PluginRequirementsValidator.unloadIncompatiblePlugins(plugins);

    assertThat(plugins).contains(Map.entry(pluginE.getKey(), pluginE));
  }

  @Test
  public void isCompatible_verifies_base_plugin_existence() {
    PluginInfo pluginWithoutBase = new PluginInfo("plugin-without-base-plugin")
      .setBasePlugin("not-existing-base-plugin");
    PluginInfo basePlugin = new PluginInfo("base-plugin");
    PluginInfo pluginWithBase = new PluginInfo("plugin-with-base-plugin")
      .setBasePlugin("base-plugin");
    Map<String, PluginInfo> plugins = new HashMap<>();
    plugins.put(pluginWithoutBase.getKey(), pluginWithoutBase);
    plugins.put(basePlugin.getKey(), basePlugin);
    plugins.put(pluginWithBase.getKey(), pluginWithBase);

    var underTest = new PluginRequirementsValidator<>(plugins);

    assertThat(underTest.isCompatible(pluginWithoutBase)).isFalse();
    assertThat(underTest.isCompatible(pluginWithBase)).isTrue();
    assertThat(logTester.logs(Level.WARN))
      .contains("Plugin plugin-without-base-plugin [plugin-without-base-plugin] is ignored"
        + " because its base plugin [not-existing-base-plugin] is not installed");
  }

  @Test
  public void isCompatible_verifies_required_plugin_existence() {
    PluginInfo requiredPlugin = new PluginInfo("required")
      .setVersion(Version.create("1.2"));
    PluginInfo pluginWithRequired = new PluginInfo("plugin-with-required-plugin")
      .addRequiredPlugin(RequiredPlugin.parse("required:1.2"));
    PluginInfo pluginWithoutRequired = new PluginInfo("plugin-without-required-plugin")
      .addRequiredPlugin(RequiredPlugin.parse("notexistingrequired:1.0"));

    Map<String, PluginInfo> plugins = new HashMap<>();
    plugins.put(requiredPlugin.getKey(), requiredPlugin);
    plugins.put(pluginWithRequired.getKey(), pluginWithRequired);
    plugins.put(pluginWithoutRequired.getKey(), pluginWithoutRequired);

    var underTest = new PluginRequirementsValidator<>(plugins);

    assertThat(underTest.isCompatible(pluginWithoutRequired)).isFalse();
    assertThat(underTest.isCompatible(pluginWithRequired)).isTrue();
    assertThat(logTester.logs(Level.WARN))
      .contains("Plugin plugin-without-required-plugin [plugin-without-required-plugin] is ignored"
        + " because the required plugin [notexistingrequired] is not installed");
  }

  @Test
  public void isCompatible_verifies_required_plugins_version() {
    PluginInfo requiredPlugin = new PluginInfo("required")
      .setVersion(Version.create("1.2"));
    PluginInfo pluginWithRequired = new PluginInfo("plugin-with-required-plugin")
      .addRequiredPlugin(RequiredPlugin.parse("required:0.8"));
    PluginInfo pluginWithoutRequired = new PluginInfo("plugin-without-required-plugin")
      .addRequiredPlugin(RequiredPlugin.parse("required:1.5"));

    Map<String, PluginInfo> plugins = new HashMap<>();
    plugins.put(requiredPlugin.getKey(), requiredPlugin);
    plugins.put(pluginWithRequired.getKey(), pluginWithRequired);
    plugins.put(pluginWithoutRequired.getKey(), pluginWithoutRequired);

    var underTest = new PluginRequirementsValidator<>(plugins);

    assertThat(underTest.isCompatible(pluginWithoutRequired)).isFalse();
    assertThat(underTest.isCompatible(pluginWithRequired)).isTrue();
    assertThat(logTester.logs(Level.WARN))
      .contains("Plugin plugin-without-required-plugin [plugin-without-required-plugin] is ignored"
        + " because the version 1.5 of required plugin [required] is not installed");
  }

}
