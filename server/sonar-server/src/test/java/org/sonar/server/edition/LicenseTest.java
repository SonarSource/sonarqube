/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.edition;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_fails_if_editionKey_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("editionKey can't be null");

    new License(null, Collections.emptyList(), "content");
  }

  @Test
  public void constructor_fails_if_editionKey_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("editionKey can't be empty");

    new License("", Collections.emptyList(), "content");
  }

  @Test
  public void constructor_fails_if_plugins_is_null() {
    expectedException.expect(NullPointerException.class);

    new License("edition-key", null, "content");
  }

  @Test
  public void constructor_fails_if_content_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("content can't be null");

    new License("edition-key", Collections.emptyList(), null);
  }

  @Test
  public void constructor_fails_if_content_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("content can't be empty");

    new License("edition-key", Collections.emptyList(), "");
  }

  @Test
  public void parse_returns_empty_if_license_is_invalid_string() {
    assertThat(License.parse("trash")).isEmpty();
  }

  @Test
  public void parse_succeeds() throws IOException {
    Properties props = new Properties();
    props.setProperty("Plugins", "plugin1,plugin2");
    props.setProperty("Edition", "dev");
    StringWriter writer = new StringWriter();
    props.store(writer, "");

    byte[] encoded = Base64.getEncoder().encode(writer.toString().getBytes());

    Optional<License> license = License.parse(new String(encoded));
    assertThat(license).isPresent();
    assertThat(license.get().getEditionKey()).isEqualTo("dev");
    assertThat(license.get().getPluginKeys()).containsOnly("plugin1", "plugin2");
  }

  @Test
  public void parse_is_empty_if_license_has_no_plugin() throws IOException {
    Properties props = new Properties();
    props.setProperty("Plugins", "");
    props.setProperty("Edition", "dev");
    StringWriter writer = new StringWriter();
    props.store(writer, "");

    byte[] encoded = Base64.getEncoder().encode(writer.toString().getBytes());

    Optional<License> license = License.parse(new String(encoded));
    assertThat(license).isEmpty();
  }

  @Test
  public void parse_is_empty_if_license_has_empty_edition_key() throws IOException {
    Properties props = new Properties();
    props.setProperty("Plugins", "p1");
    props.setProperty("Edition", "");
    StringWriter writer = new StringWriter();
    props.store(writer, "");

    byte[] encoded = Base64.getEncoder().encode(writer.toString().getBytes());

    Optional<License> license = License.parse(new String(encoded));
    assertThat(license).isEmpty();
  }

  @Test
  public void parse_is_empty_if_license_has_no_edition_key() throws IOException {
    Properties props = new Properties();
    props.setProperty("Plugins", "plugin1,plugin2");
    StringWriter writer = new StringWriter();
    props.store(writer, "");

    byte[] encoded = Base64.getEncoder().encode(writer.toString().getBytes());

    Optional<License> license = License.parse(new String(encoded));
    assertThat(license).isEmpty();
  }

  @Test
  public void verify_getters() {
    ImmutableSet<String> pluginKeys = ImmutableSet.of("a", "b", "c");

    License underTest = new License("edition-key", pluginKeys, "content");

    assertThat(underTest.getEditionKey()).isEqualTo("edition-key");
    assertThat(underTest.getPluginKeys()).isEqualTo(pluginKeys);
    assertThat(underTest.getContent()).isEqualTo("content");
  }
}
