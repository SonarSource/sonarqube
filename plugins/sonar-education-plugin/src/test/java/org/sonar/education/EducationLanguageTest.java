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
package org.sonar.education;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EducationLanguageTest {

  private final EducationLanguage underTest = new EducationLanguage();

  @Test
  public void getFileSuffixes_notEmpty() {
    String[] fileSuffixes = underTest.getFileSuffixes();

    assertThat(fileSuffixes).isNotEmpty();
  }

  @Test
  public void getName_notEmpty() {
    String name = underTest.getName();

    assertThat(name).isNotEmpty();
  }

  @Test
  public void getKey_notEmpty() {
    String key = underTest.getKey();

    assertThat(key).isNotEmpty();
  }
}
