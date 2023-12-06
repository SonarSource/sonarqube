/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

public class SupportedLanguageDto implements org.sonar.api.resources.Language {
  private String key;
  private String name;
  private String[] fileSuffixes;
  private String[] filenamePatterns;

  public SupportedLanguageDto(String key, String name, String[] fileSuffixes, String[] filenamePatterns) {
    this.key = key;
    this.name = name;
    this.fileSuffixes = fileSuffixes;
    this.filenamePatterns = filenamePatterns;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String[] getFileSuffixes() {
    return fileSuffixes;
  }

  public void setFileSuffixes(String[] fileSuffixes) {
    this.fileSuffixes = fileSuffixes;
  }

  @Override
  public String[] filenamePatterns() {
    return filenamePatterns;
  }

  public void setFilenamePatterns(String[] filenamePatterns) {
    this.filenamePatterns = filenamePatterns;
  }

}
