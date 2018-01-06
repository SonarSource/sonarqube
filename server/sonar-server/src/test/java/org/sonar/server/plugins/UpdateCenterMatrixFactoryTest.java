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
package org.sonar.server.plugins;

import com.google.common.base.Optional;
import org.junit.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.updatecenter.common.UpdateCenter;

import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdateCenterMatrixFactoryTest {

  private UpdateCenterMatrixFactory underTest;

  @Test
  public void return_absent_update_center() {
    UpdateCenterClient updateCenterClient = mock(UpdateCenterClient.class);
    when(updateCenterClient.getUpdateCenter(anyBoolean())).thenReturn(Optional.absent());

    underTest = new UpdateCenterMatrixFactory(updateCenterClient, mock(SonarRuntime.class), mock(InstalledPluginReferentialFactory.class));

    Optional<UpdateCenter> updateCenter = underTest.getUpdateCenter(false);

    assertThat(updateCenter).isAbsent();
  }
}
