/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.config;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EncryptionTest {

  @Test
  public void isEncrypted() {
    Encryption encryption = new Encryption(new Settings());
    assertThat(encryption.isEncrypted("{aes}ADASDASAD"), is(true));
    assertThat(encryption.isEncrypted("{b64}ADASDASAD"), is(true));
    assertThat(encryption.isEncrypted("{abc}ADASDASAD"), is(true));

    assertThat(encryption.isEncrypted("{}"), is(false));
    assertThat(encryption.isEncrypted("{foo"), is(false));
    assertThat(encryption.isEncrypted("foo{aes}"), is(false));
  }

  @Test
  public void scramble() {
    Encryption encryption = new Encryption(new Settings());
    assertThat(encryption.scramble("foo"), is("{b64}Zm9v"));
  }

  @Test
  public void decrypt() {
    Encryption encryption = new Encryption(new Settings());
    assertThat(encryption.decrypt("{b64}Zm9v"), is("foo"));
  }

  @Test
  public void decrypt_unknown_algorithm() {
    Encryption encryption = new Encryption(new Settings());
    assertThat(encryption.decrypt("{xxx}Zm9v"), is("{xxx}Zm9v"));
  }

  @Test
  public void decrypt_uncrypted_text() {
    Encryption encryption = new Encryption(new Settings());
    assertThat(encryption.decrypt("foo"), is("foo"));
  }
}
