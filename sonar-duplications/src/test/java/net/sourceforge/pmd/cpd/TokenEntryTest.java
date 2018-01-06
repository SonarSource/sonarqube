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
package net.sourceforge.pmd.cpd;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class TokenEntryTest {

  @Before
  public void setUp() {
    TokenEntry.clearImages();
  }

  @Test
  public void testNewTokenEntry() {
    TokenEntry entry = new TokenEntry("token1", "src1", 1);
    assertThat(entry.getValue(), equalTo("token1"));
    assertThat(entry.getBeginLine(), equalTo(1));

    entry = new TokenEntry("token2", "src2", 2);
    assertThat(entry.getValue(), equalTo("token2"));
    assertThat(entry.getBeginLine(), equalTo(2));
  }

  @Test
  public void testGetEOF() {
    assertThat(TokenEntry.getEOF(), sameInstance(TokenEntry.getEOF()));
  }

}
