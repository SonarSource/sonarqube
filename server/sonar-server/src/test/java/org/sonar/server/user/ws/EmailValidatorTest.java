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
package org.sonar.server.user.ws;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.user.ws.EmailValidator.isValidIfPresent;

public class EmailValidatorTest {

  @Test
  public void valid_if_absent_or_empty() {
    assertThat(isValidIfPresent(null)).isTrue();
    assertThat(isValidIfPresent("")).isTrue();
  }

  @Test
  public void various_examples_of_unusual_but_valid_emails() {
    assertThat(isValidIfPresent("info@sonarsource.com")).isTrue();
    assertThat(isValidIfPresent("guillaume.jambet+sonarsource-emailvalidatortest@gmail.com")).isTrue();
    assertThat(isValidIfPresent("webmaster@kiné-beauté.fr")).isTrue();
    assertThat(isValidIfPresent("\"Fred Bloggs\"@example.com")).isTrue();
    assertThat(isValidIfPresent("Chuck Norris <coup-de-pied-retourné@chucknorris.com>")).isTrue();
    assertThat(isValidIfPresent("pipo@127.0.0.1")).isTrue();
    assertThat(isValidIfPresent("admin@admin")).isTrue();
  }

  @Test
  public void various_examples_of_invalid_emails() {
    assertThat(isValidIfPresent("infosonarsource.com")).isFalse();
    assertThat(isValidIfPresent("info@.sonarsource.com")).isFalse();
    assertThat(isValidIfPresent("info\"@.sonarsource.com")).isFalse();
  }

}
