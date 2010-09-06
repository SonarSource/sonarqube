/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.checks.templates;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class DefaultCheckTemplateTest {

  @Test
  public void isNotInternationalized() {
    DefaultCheckTemplate check = new DefaultCheckTemplate("key1");
    check.setTitle("title");
    check.setDescription("desc");

    assertEquals("title", check.getTitle(Locale.ENGLISH));
    assertEquals(check.getTitle(Locale.ENGLISH), check.getTitle(Locale.FRENCH));

    assertEquals("desc", check.getDescription(Locale.ENGLISH));
    assertEquals(check.getDescription(Locale.ENGLISH), check.getDescription(Locale.FRENCH));
  }
}
