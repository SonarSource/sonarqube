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
package org.sonar.api.batch.fs.internal;

import java.util.Locale;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

/**
 * @since 6.3
 */
public class FileExtensionPredicate extends AbstractFilePredicate {

  private final String extension;

  public FileExtensionPredicate(String extension) {
    this.extension = lowercase(extension);
  }

  @Override
  public boolean apply(InputFile inputFile) {
    return extension.equals(getExtension(inputFile));
  }

  @Override
  public Iterable<InputFile> get(FileSystem.Index index) {
    return index.getFilesByExtension(extension);
  }

  public static String getExtension(InputFile inputFile) {
    return getExtension(inputFile.filename());
  }

  static String getExtension(String name) {
    int index = name.lastIndexOf('.');
    if (index < 0) {
      return "";
    }
    return lowercase(name.substring(index + 1));
  }

  private static String lowercase(String extension) {
    return extension.toLowerCase(Locale.ENGLISH);
  }
}
