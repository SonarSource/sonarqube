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
package org.sonar.ce.task.projectanalysis.component;

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class DisabledComponentsHolderImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  DisabledComponentsHolderImpl underTest = new DisabledComponentsHolderImpl();

  @Test
  public void set_and_get_uuids() {
    underTest.setUuids(ImmutableSet.of("U1", "U2"));

    assertThat(underTest.getUuids()).containsExactly("U1", "U2");
  }

  @Test
  public void setUuids_fails_if_called_twice() {
    underTest.setUuids(ImmutableSet.of("U1", "U2"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("UUIDs have already been set in repository");
    underTest.setUuids(ImmutableSet.of("U1", "U2"));
  }

  @Test
  public void getUuids_fails_if_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("UUIDs have not been set in repository");
    underTest.getUuids();
  }
}
