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
package org.sonar.server.almsettings;

import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultipleAlmFeatureProviderTest {

  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private final MultipleAlmFeatureProvider underTest = new MultipleAlmFeatureProvider(editionProvider);

  @Test
  public void is_enabled_on_CE() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));
    Assertions.assertThat(underTest.enabled()).isFalse();
  }

  @Test
  public void is_enabled_on_DE() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));
    Assertions.assertThat(underTest.enabled()).isFalse();
  }

  @Test
  public void is_enabled_on_EE() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.ENTERPRISE));
    Assertions.assertThat(underTest.enabled()).isTrue();
  }

  @Test
  public void is_enabled_on_DCE() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DATACENTER));
    Assertions.assertThat(underTest.enabled()).isTrue();
  }
}
