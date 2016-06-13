/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.internal;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SonarRuntimeFactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create() {
    SonarQubeVersion version = SonarRuntimeFactory.create(System2.INSTANCE, SonarProduct.SONARLINT, null);
    assertThat(version).isNotNull();
    assertThat(version.getApiVersion().major()).isGreaterThanOrEqualTo(5);
    assertThat(version.getProduct()).isEqualTo(SonarProduct.SONARLINT);
  }

  @Test
  public void throw_ISE_if_fail_to_load_version() throws Exception {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not load /sq-version.txt from classpath");

    System2 system = spy(System2.class);
    when(system.getResource(anyString())).thenReturn(new File("target/unknown").toURI().toURL());
    SonarRuntimeFactory.create(system, SonarProduct.SONARLINT, null);
  }

}
