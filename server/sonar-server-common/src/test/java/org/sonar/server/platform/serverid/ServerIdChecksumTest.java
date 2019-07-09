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
package org.sonar.server.platform.serverid;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;

public class ServerIdChecksumTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void compute_throws_ISE_if_jdbcUrl_property_is_not_set() {
    ServerIdChecksum underTest = new ServerIdChecksum(new MapSettings().asConfig(), null /*doesn't matter*/);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Missing JDBC URL");

    underTest.computeFor("foo");
  }

  @Test
  public void test_checksum() {
    assertThat(computeFor("id1", "url1"))
      .isNotEmpty()
      .isEqualTo(computeFor("id1", "url1"))
      .isNotEqualTo(computeFor("id1", "url2"))
      .isNotEqualTo(computeFor("id2", "url1"))
      .isNotEqualTo(computeFor("id2", "url2"));
  }

  private String computeFor(String serverId, String jdbcUrl) {
    MapSettings settings = new MapSettings();
    JdbcUrlSanitizer jdbcUrlSanitizer = mock(JdbcUrlSanitizer.class);
    when(jdbcUrlSanitizer.sanitize(jdbcUrl)).thenReturn("_" + jdbcUrl);
    ServerIdChecksum underTest = new ServerIdChecksum(settings.asConfig(), jdbcUrlSanitizer);
    settings.setProperty(JDBC_URL.getKey(), jdbcUrl);
    return underTest.computeFor(serverId);
  }
}
