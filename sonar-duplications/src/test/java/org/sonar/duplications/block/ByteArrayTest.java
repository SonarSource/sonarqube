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
package org.sonar.duplications.block;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ByteArrayTest {

  @Test
  public void shouldCreateFromInt() {
    int value = 0x12FF8413;
    ByteArray byteArray = new ByteArray(value);
    assertThat(byteArray.toString(), is(Integer.toHexString(value)));
  }

  @Test
  public void shouldCreateFromLong() {
    long value = 0x12FF841344567899L;
    ByteArray byteArray = new ByteArray(value);
    assertThat(byteArray.toString(), is(Long.toHexString(value)));
  }

  @Test
  public void shouldCreateFromHexString() {
    String value = "12FF841344567899";
    ByteArray byteArray = new ByteArray(value);
    assertThat(byteArray.toString(), is(value.toLowerCase()));
  }

  @Test
  public void shouldCreateFromIntArray() {
    ByteArray byteArray = new ByteArray(new int[] { 0x04121986 });
    assertThat(byteArray.toString(), is("04121986"));
  }

  @Test
  public void shouldConvertToIntArray() {
    // number of bytes is enough to create exactly one int (4 bytes)
    ByteArray byteArray = new ByteArray(new byte[] { 0x04, 0x12, 0x19, (byte) 0x86 });
    assertThat(byteArray.toIntArray(), is(new int[] { 0x04121986 }));
    // number of bytes is more than 4, but less than 8, so anyway 2 ints
    byteArray = new ByteArray(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x31 });
    assertThat(byteArray.toIntArray(), is(new int[] { 0x00000000, 0x31000000 }));
  }

}
