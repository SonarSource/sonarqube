/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class AbstractLanguageTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void aLanguageShouldEqualItselft() {
    assertEquals(new Java(), new Java());
  }

  @Test
  public void shouldNotDefineLanguageWithTooLongKey() {
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

}
