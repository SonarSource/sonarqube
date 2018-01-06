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
package org.sonar.duplications.token;

import org.junit.Test;
import org.sonar.channel.CodeReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class BlackHoleTokenChannelTest {

  @Test
  public void shouldConsume() {
    BlackHoleTokenChannel channel = new BlackHoleTokenChannel("ABC");
    TokenQueue output = mock(TokenQueue.class);
    CodeReader codeReader = new CodeReader("ABCD");

    assertThat(channel.consume(codeReader, output)).isTrue();
    assertThat(codeReader.getLinePosition()).isEqualTo(1);
    assertThat(codeReader.getColumnPosition()).isEqualTo(3);
    verifyZeroInteractions(output);
  }

}
