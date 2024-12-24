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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScannerPropertiesTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void initialization() {
    ImmutableMap<String, String> map = ImmutableMap.<String, String>builder()
      .put("prop-1", "{b64}Zm9v")
      .put("sonar.projectKey", "my-project")
      .build();
    ScannerProperties underTest = new ScannerProperties(map);

    assertThat(underTest.getEncryption()).isNotNull();
    assertThat(underTest.properties())
      .containsEntry("prop-1", "foo")
      .containsEntry("sonar.projectKey", "my-project");
    assertThat(underTest.getProjectKey()).isEqualTo("my-project");
  }

  @Test
  void encryption_fail() {
    ImmutableMap<String, String> map = ImmutableMap.<String, String>builder()
      .put("prop-1", "{aes}Zm9vzxc")
      .build();
    assertThatThrownBy(() -> new ScannerProperties(map))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to decrypt the property");
  }

  @Test
  void encryption_ok() {
    ImmutableMap<String, String> map = ImmutableMap.<String, String>builder()
      .put("prop-1", "{b64}Zm9v")
      .build();
    ScannerProperties underTest = new ScannerProperties(map);

    assertThat(underTest.property("prop-1")).isEqualTo("foo");

    assertThat(logTester.logs(Level.WARN))
      .contains(
        "Property 'prop-1' is encrypted. The encryption of scanner properties is deprecated and will soon be removed.");
  }

}
