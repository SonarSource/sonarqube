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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.Test;
import org.sonar.scanner.scan.EmptyExternalProjectKeyAndOrganization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class ProcessedScannerPropertiesTest {
  @Test
  public void test_copy_of_properties() {
    Map<String, String> map = Maps.newHashMap();
    map.put("foo", "bar");

    ProcessedScannerProperties underTest = new ProcessedScannerProperties(
      new RawScannerProperties(map),
      new EmptyExternalProjectKeyAndOrganization());
    assertThat(underTest.properties()).containsOnly(entry("foo", "bar"));
    assertThat(underTest.properties()).isNotSameAs(map);

    map.put("put", "after_copy");
    assertThat(underTest.properties()).hasSize(1);
  }
}
