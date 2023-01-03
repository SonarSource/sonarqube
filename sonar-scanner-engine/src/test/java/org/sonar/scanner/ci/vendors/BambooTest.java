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
package org.sonar.scanner.ci.vendors;

import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiVendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BambooTest {

  private final System2 system = mock(System2.class);
  private final CiVendor underTest = new Bamboo(system);

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("Bamboo");
  }

  @Test
  public void isDetected() {
    assertThat(underTest.isDetected()).isFalse();

    setEnvVariable("bamboo_buildNumber", "41");
    assertThat(underTest.isDetected()).isTrue();
  }

  @Test
  public void loadConfiguration() {
    setEnvVariable("bamboo_buildNumber", "41");
    setEnvVariable("bamboo_planRepository_revision", "42109719-93c9-4d47-9d7c-b3b0b87b6985");

    assertThat(underTest.loadConfiguration().getScmRevision()).hasValue("42109719-93c9-4d47-9d7c-b3b0b87b6985");
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }
}
