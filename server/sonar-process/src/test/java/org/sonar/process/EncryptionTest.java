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
package org.sonar.process;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class EncryptionTest {

  @Test
  public void isEncrypted() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.isEncrypted("{aes}ADASDASAD")).isTrue();
    assertThat(encryption.isEncrypted("{b64}ADASDASAD")).isTrue();
    assertThat(encryption.isEncrypted("{abc}ADASDASAD")).isTrue();

    assertThat(encryption.isEncrypted("{}")).isFalse();
    assertThat(encryption.isEncrypted("{foo")).isFalse();
    assertThat(encryption.isEncrypted("foo{aes}")).isFalse();
  }

  @Test
  public void decrypt() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.decrypt("{b64}Zm9v")).isEqualTo("foo");
  }

  @Test
  public void decrypt_unknown_algorithm() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.decrypt("{xxx}Zm9v")).isEqualTo("{xxx}Zm9v");
  }

  @Test
  public void decrypt_uncrypted_text() {
    Encryption encryption = new Encryption(null);
    assertThat(encryption.decrypt("foo")).isEqualTo("foo");
  }
}
