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
package org.sonar.server.platform.monitoring;

import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class SettingsSectionTest {

  private static final String PASSWORD_PROPERTY = "sonar.password";

  private PropertyDefinitions defs = new PropertyDefinitions(PropertyDefinition.builder(PASSWORD_PROPERTY).type(PropertyType.PASSWORD).build());
  private Settings settings = new MapSettings(defs);
  private SettingsSection underTest = new SettingsSection(settings);

  @Test
  public void return_properties_and_sort_by_key() {
    settings.setProperty("foo", "foo value");
    settings.setProperty("bar", "bar value");

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, "bar", "bar value");
    assertThatAttributeIs(protobuf, "foo", "foo value");

    // keys are ordered alphabetically
    assertThat(protobuf.getAttributesList())
      .extracting(ProtobufSystemInfo.Attribute::getKey)
      .containsExactly("bar", "foo");
  }

  @Test
  public void truncate_long_property_values() {
    settings.setProperty("foo", repeat("abcde", 1_000));

    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    String value = attribute(protobuf, "foo").getStringValue();
    assertThat(value).hasSize(500).startsWith("abcde");
  }

  @Test
  public void value_is_obfuscated_if_key_matches_patterns() {
    verifyObfuscated(PASSWORD_PROPERTY);
    verifyObfuscated("foo.password.something");
    // case insensitive search of "password" term
    verifyObfuscated("bar.CheckPassword");
    verifyObfuscated("foo.passcode.something");
    // case insensitive search of "passcode" term
    verifyObfuscated("bar.CheckPassCode");
    verifyObfuscated("foo.something.secured");
    verifyObfuscated("bar.something.Secured");
    verifyObfuscated("sonar.auth.jwtBase64Hs256Secret");

    verifyNotObfuscated("securedStuff");
    verifyNotObfuscated("foo");
  }

  private void verifyObfuscated(String key) {
    settings.setProperty(key, "foo");
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, key, "xxxxxxxx");
  }

  private void verifyNotObfuscated(String key) {
    settings.setProperty(key, "foo");
    ProtobufSystemInfo.Section protobuf = underTest.toProtobuf();
    assertThatAttributeIs(protobuf, key, "foo");
  }

  @Test
  public void test_monitor_name() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Settings");
  }
}
