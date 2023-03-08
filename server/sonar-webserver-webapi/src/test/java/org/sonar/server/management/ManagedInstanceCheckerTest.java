/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.management;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManagedInstanceCheckerTest {

  @Mock
  private ManagedInstanceService managedInstanceService;

  @InjectMocks
  private ManagedInstanceChecker managedInstanceChecker;

  @Test
  public void throwIfInstanceIsManaged_whenInstanceExternallyManaged_throws() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    assertThatThrownBy(() -> managedInstanceChecker.throwIfInstanceIsManaged())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Operation not allowed when the instance is externally managed.");
  }

  @Test
  public void throwIfInstanceIsManaged_whenInstanceNotExternallyManaged_doesntThrow() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(false);
    assertThatNoException().isThrownBy(() -> managedInstanceChecker.throwIfInstanceIsManaged());
  }
}
