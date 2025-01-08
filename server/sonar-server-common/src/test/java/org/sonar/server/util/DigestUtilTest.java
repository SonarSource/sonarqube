/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DigestUtilTest {

  @Test
  void whenCalledOnTheSameInput_sha3_224Hex_returnsTheSameValueAsDigestUtils() {
    String input = "String to be hashed";

    //Apache Commons DigestUtils digest for the string "String to be hashed"
    String apacheCommonsDigest = "57ead8c5fc5c15ed7bde0550648d06a6aed3cba443ed100a6f5e64f3";

    assertEquals(apacheCommonsDigest, DigestUtil.sha3_224Hex(input));
  }

}