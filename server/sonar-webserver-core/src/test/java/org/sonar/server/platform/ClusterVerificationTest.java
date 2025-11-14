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
package org.sonar.server.platform;

import org.junit.Test;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClusterVerificationTest {

  private static final String ERROR_MESSAGE = "Cluster mode can't be enabled. Please install the Data Center Edition. More details at https://www.sonarsource.com/plans-and-pricing/data-center/.";


  private NodeInformation nodeInformation = mock(NodeInformation.class);
  private ClusterFeature feature = mock(ClusterFeature.class);

  @Test
  public void throw_MessageException_if_cluster_is_enabled_but_HA_plugin_is_not_installed() {
    when(nodeInformation.isStandalone()).thenReturn(false);

    ClusterVerification underTest = new ClusterVerification(nodeInformation);

    assertThatThrownBy(underTest::start)
      .isInstanceOf(MessageException.class)
      .hasMessage(ERROR_MESSAGE);
  }

  @Test
  public void throw_MessageException_if_cluster_is_enabled_but_HA_feature_is_not_enabled() {
    when(nodeInformation.isStandalone()).thenReturn(false);
    when(feature.isEnabled()).thenReturn(false);
    ClusterVerification underTest = new ClusterVerification(nodeInformation, feature);

    assertThatThrownBy(underTest::start)
      .isInstanceOf(MessageException.class)
      .hasMessage(ERROR_MESSAGE);
  }

  @Test
  public void do_not_fail_if_cluster_is_enabled_and_HA_feature_is_enabled() {
    when(nodeInformation.isStandalone()).thenReturn(false);
    when(feature.isEnabled()).thenReturn(true);
    ClusterVerification underTest = new ClusterVerification(nodeInformation, feature);

    // no failure
    underTest.start();
    underTest.stop();
  }

  @Test
  public void do_not_fail_if_cluster_is_disabled() {
    when(nodeInformation.isStandalone()).thenReturn(true);

    ClusterVerification underTest = new ClusterVerification(nodeInformation);

    // no failure
    underTest.start();
    underTest.stop();
  }


}
