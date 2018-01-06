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
package org.sonar.scanner.repository.language;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class Language {

  private final String key;
  private final String name;
  private final String[] fileSuffixes;

  public Language(String key, String name, String... fileSuffixes) {
    this.key = key;
    this.name = name;
    this.fileSuffixes = fileSuffixes;
  }

  /**
   * For example "java".
   */
  public String key() {
    return key;
  }

  /**
   * For example "Java"
   */
  public String name() {
    return name;
  }

  /**
   * For example ["jav", "java"].
   */
  public Collection<String> fileSuffixes() {
    return Arrays.asList(fileSuffixes);
  }

  @Override
  public String toString() {
    return name;
  }

}
