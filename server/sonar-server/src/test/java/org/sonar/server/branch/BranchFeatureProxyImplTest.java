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
package org.sonar.server.branch;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BranchFeatureProxyImplTest {

  private BranchFeatureExtension branchFeatureExtension = mock(BranchFeatureExtension.class);

  @Test
  public void return_false_when_no_extension() {
    assertThat(new BranchFeatureProxyImpl().isEnabled()).isFalse();
  }

  @Test
  public void return_false_when_extension_returns_false() {
    when(branchFeatureExtension.isEnabled()).thenReturn(false);
    assertThat(new BranchFeatureProxyImpl(branchFeatureExtension).isEnabled()).isFalse();
  }

  @Test
  public void return_true_when_extension_returns_ftrue() {
    when(branchFeatureExtension.isEnabled()).thenReturn(true);
    assertThat(new BranchFeatureProxyImpl(branchFeatureExtension).isEnabled()).isTrue();
  }
}
