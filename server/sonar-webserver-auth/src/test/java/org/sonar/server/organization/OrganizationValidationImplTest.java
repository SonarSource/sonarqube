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
package org.sonar.server.organization;

import com.google.common.base.Strings;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class OrganizationValidationImplTest {
  private static final String STRING_32_CHARS = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
  private static final String STRING_64_CHARS = STRING_32_CHARS + STRING_32_CHARS;
  private static final String STRING_256_CHARS = STRING_64_CHARS + STRING_64_CHARS + STRING_64_CHARS + STRING_64_CHARS;

  private static final String STRING_255_CHARS = Strings.repeat("a", 255);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private OrganizationValidationImpl underTest = new OrganizationValidationImpl();

  @Test
  public void checkValidKey_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key can't be null");

    underTest.checkKey(null);
  }

  @Test
  public void checkValidKey_throws_IAE_if_arg_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key must not be empty");

    underTest.checkKey("");
  }

  @Test
  public void checkValidKey_throws_IAE_if_key_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key must not be empty");

    underTest.checkKey("");
  }

  @Test
  public void checkValidKey_does_not_fail_if_arg_is_1_to_255_chars_long() {
    String str = "a";
    for (int i = 0; i < 254; i++) {
      underTest.checkKey(str);
      str += "a";
    }
  }

  @Test
  public void checkValidKey_throws_IAE_when_more_than_300_characters() {
    String key = STRING_255_CHARS + "b";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Key '" + key + "' must be at most 255 chars long");

    underTest.checkKey(key);
  }

  @Test
  public void checkValidKey_throws_IAE_if_arg_contains_invalid_chars() {
    char[] invalidChars = {'é', '<', '@'};

    for (char invalidChar : invalidChars) {
      String str = "aa" + invalidChar;
      try {
        underTest.checkKey(str);
        fail("A IllegalArgumentException should have been thrown");
      } catch (IllegalArgumentException e) {
        assertThat(e).hasMessage("Key '" + str + "' contains at least one invalid char");
      }
    }
  }

  @Test
  public void checkValidName_throws_NPE_if_arg_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    underTest.checkName(null);
  }

  @Test
  public void checkValidName_throws_IAE_if_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name must not be empty");

    underTest.checkName("");
  }

  @Test
  public void checkValidName_does_not_fail_if_arg_is_1_to_255_chars_long() {
    String str = "a";
    for (int i = 0; i < 254; i++) {
      underTest.checkName(str);
      str += "a";
    }
  }

  @Test
  public void checkValidName_throws_IAE_when_more_than_255_characters() {
    String str = STRING_255_CHARS + "b";

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name '" + str + "' must be at most 255 chars long");

    underTest.checkName(str);
  }

  @Test
  public void checkValidDescription_does_not_fail_if_arg_is_null() {
    underTest.checkDescription(null);
  }

  @Test
  public void checkValidDescription_does_not_fail_if_arg_is_empty() {
    underTest.checkDescription("");
  }

  @Test
  public void checkValidDescription_does_not_fail_if_arg_is_1_to_256_chars_long() {
    String str = "1";
    for (int i = 0; i < 256; i++) {
      underTest.checkDescription(str);
      str += "a";
    }
  }

  @Test
  public void checkValidDescription_throws_IAE_if_arg_is_more_than_256_chars_long() {
    String str = STRING_256_CHARS;
    underTest.checkDescription(str);
    for (int i = 0; i < 5 + Math.abs(new Random().nextInt(10)); i++) {
      str += "c";
      try {
        underTest.checkDescription(str);
        fail("A IllegalArgumentException should have been thrown");
      } catch (IllegalArgumentException e) {
        assertThat(e).hasMessage("Description '" + str + "' must be at most 256 chars long");
      }
    }
  }

  @Test
  public void checkValidUrl_does_not_fail_if_arg_is_null() {
    underTest.checkUrl(null);
  }

  @Test
  public void checkValidUrl_does_not_fail_if_arg_is_1_to_256_chars_long() {
    String str = "1";
    for (int i = 0; i < 256; i++) {
      underTest.checkUrl(str);
      str += "a";
    }
  }

  @Test
  public void checkValidUrl_throws_IAE_if_arg_is_more_than_256_chars_long() {
    String str = STRING_256_CHARS;
    underTest.checkUrl(str);
    for (int i = 0; i < 5 + Math.abs(new Random().nextInt(10)); i++) {
      str += "c";
      try {
        underTest.checkUrl(str);
        fail("A IllegalArgumentException should have been thrown");
      } catch (IllegalArgumentException e) {
        assertThat(e).hasMessage("Url '" + str + "' must be at most 256 chars long");
      }
    }
  }

  @Test
  public void checkValidAvatar_does_not_fail_if_arg_is_null() {
    underTest.checkAvatar(null);
  }

  @Test
  public void checkValidAvatar_does_not_fail_if_arg_is_1_to_256_chars_long() {
    String str = "1";
    for (int i = 0; i < 256; i++) {
      underTest.checkAvatar(str);
      str += "a";
    }
  }

  @Test
  public void checkValidAvatar_throws_IAE_if_arg_is_more_than_256_chars_long() {
    String str = STRING_256_CHARS;
    underTest.checkAvatar(str);
    for (int i = 0; i < 5 + Math.abs(new Random().nextInt(10)); i++) {
      str += "c";
      try {
        underTest.checkAvatar(str);
        fail("A IllegalArgumentException should have been thrown");
      } catch (IllegalArgumentException e) {
        assertThat(e).hasMessage("Avatar '" + str + "' must be at most 256 chars long");
      }
    }
  }

  @Test
  public void generateKeyFrom_returns_slug_of_arg() {
    assertThat(underTest.generateKeyFrom("foo")).isEqualTo("foo");
    assertThat(underTest.generateKeyFrom("  FOO ")).isEqualTo("foo");
    assertThat(underTest.generateKeyFrom("he's here")).isEqualTo("he-s-here");
    assertThat(underTest.generateKeyFrom("foo-bar")).isEqualTo("foo-bar");
    assertThat(underTest.generateKeyFrom("foo_bar")).isEqualTo("foo_bar");
    assertThat(underTest.generateKeyFrom("accents éà")).isEqualTo("accents-ea");
    assertThat(underTest.generateKeyFrom("<foo>")).isEqualTo("foo");
    assertThat(underTest.generateKeyFrom("<\"foo:\">")).isEqualTo("foo");
  }

}
