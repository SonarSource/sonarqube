/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.duplications.cpd;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class FileCodeLoaderWithoutCache extends CodeLoaderWithoutCache {

  private File file;
  private String encoding;

  public FileCodeLoaderWithoutCache(File file, String encoding) {
    this.file = file;
    this.encoding = encoding;
  }

  @Override
  public Reader getReader() throws Exception {
    return new InputStreamReader(new FileInputStream(file), encoding);
  }

  @Override
  public String getFileName() {
    return this.file.getAbsolutePath();
  }
}
