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
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractLanguageTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_equals_and_hashcode() {
    final Language1 lang1 = new Language1();
    assertThat(lang1.equals(lang1)).isTrue();
    assertThat(lang1.equals(new Language2())).isFalse();
    assertThat(lang1.equals(new Language1Too())).isTrue();
    assertThat(lang1.equals("not a language")).isFalse();
    assertThat(lang1.equals(null)).isFalse();

    // not an AbstractLanguage but a Language
    assertThat(lang1.equals(new Language() {
      @Override
      public String getKey() {
        return lang1.getKey();
      }

      @Override
      public String getName() {
        return lang1.getName();
      }

      @Override
      public String[] getFileSuffixes() {
        return lang1.getFileSuffixes();
      }
    })).isTrue();

    assertThat(lang1.hashCode()).isEqualTo(lang1.hashCode());
    assertThat(lang1.hashCode()).isEqualTo(new Language1Too().hashCode());
  }

  @Test
  public void should_not_define_language_with_too_long_key() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("The following language key exceeds 20 characters: 'aKeyWhichIsVeryVeryVeryVeryVeryLong'");

    new TooLongKeyLanguage();
  }

  class TooLongKeyLanguage extends AbstractLanguage {
    public TooLongKeyLanguage() {
      super("aKeyWhichIsVeryVeryVeryVeryVeryLong");
    }

    public String[] getFileSuffixes() {
      // TODO Auto-generated method stub
      return null;
    }
  }

  static class Language1 extends AbstractLanguage {
    public Language1() {
      super("lang1");
    }

    public String[] getFileSuffixes() {
      return new String[0];
    }
  }

  static class Language1Too extends AbstractLanguage {
    public Language1Too() {
      super("lang1");
    }

    public String[] getFileSuffixes() {
      return new String[0];
    }
  }

  static class Language2 extends AbstractLanguage {
    public Language2() {
      super("lang2");
    }

    public String[] getFileSuffixes() {
      return new String[0];
    }
  }
}
