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
package org.sonar.scanner.issue.tracking;

import org.junit.Test;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.assertj.core.api.Assertions.assertThat;

public class RollingFileHashesTest {

  @Test
  public void test_equals() {
    RollingFileHashes a = RollingFileHashes.create(FileHashes.create(new String[] {md5Hex("line0"), md5Hex("line1"), md5Hex("line2")}), 1);
    RollingFileHashes b = RollingFileHashes.create(FileHashes.create(new String[] {md5Hex("line0"), md5Hex("line1"), md5Hex("line2"), md5Hex("line3")}), 1);

    assertThat(a.getHash(1) == b.getHash(1)).isTrue();
    assertThat(a.getHash(2) == b.getHash(2)).isTrue();
    assertThat(a.getHash(3) == b.getHash(3)).isFalse();

    RollingFileHashes c = RollingFileHashes.create(FileHashes.create(new String[] {md5Hex("line-1"), md5Hex("line0"), md5Hex("line1"), md5Hex("line2"), md5Hex("line3")}), 1);
    assertThat(a.getHash(1) == c.getHash(2)).isFalse();
    assertThat(a.getHash(2) == c.getHash(3)).isTrue();
  }

}
