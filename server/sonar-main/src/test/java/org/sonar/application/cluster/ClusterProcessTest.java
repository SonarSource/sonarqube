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
package org.sonar.application.cluster;

import org.junit.Test;
import org.sonar.process.ProcessId;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterProcessTest {
  @Test
  public void test_equality() {
    ClusterProcess clusterProcess = new ClusterProcess("A", ProcessId.WEB_SERVER);

    assertThat(clusterProcess)
      .isNotEqualTo(null)
      .isEqualTo(clusterProcess)
      .isNotEqualTo(new ClusterProcess("B", ProcessId.WEB_SERVER))
      .isNotEqualTo(new ClusterProcess("A", ProcessId.ELASTICSEARCH))
      .isEqualTo(new ClusterProcess("A", ProcessId.WEB_SERVER));
  }
}
