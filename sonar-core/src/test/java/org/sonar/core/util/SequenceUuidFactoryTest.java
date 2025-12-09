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
package org.sonar.core.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class SequenceUuidFactoryTest {

  private final SequenceUuidFactory underTest = new SequenceUuidFactory();

  @Test
  public void generate_sequence_of_integer_ids() {
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000001");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000002");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000003");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000004");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000005");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000006");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000007");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000008");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000009");
    assertThat(underTest.create()).isEqualTo("00000000-0000-0000-0000-000000000010");
  }

}
