/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class AbstractLanguageTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void a_language_should_equal_itself() {
    assertEquals(new Language1(), new Language1());
  }

  @Test
  public void should_be_equal_to_another_language_implementation_having_same_key() {
    assertThat(new Language1()).isEqualTo(new Language2());
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
      super("language_key");
    }

    public String[] getFileSuffixes() {
      return new String[0];
    }
  }

  static class Language2 extends AbstractLanguage {
    public Language2() {
      super("language_key");
    }

    public String[] getFileSuffixes() {
      return new String[0];
    }
  }

}
