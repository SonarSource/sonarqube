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

package net.sourceforge.pmd.cpd;

import java.io.File;
import java.io.FilenameFilter;

public abstract class AbstractLanguage implements Language {
  private final Tokenizer tokenizer;
  private final FilenameFilter fileFilter;

  public AbstractLanguage(Tokenizer tokenizer, String... extensions) {
    this.tokenizer = tokenizer;
    fileFilter = new ExtensionsFilter(extensions);
  }

  /**
   * @deprecated in 2.14, seems that not used in Sonar ecosystem - we don't scan directories.
   */
  public FilenameFilter getFileFilter() {
    return fileFilter;
  }

  public Tokenizer getTokenizer() {
    return tokenizer;
  }

  private static class ExtensionsFilter implements FilenameFilter {
    private final String[] extensions;

    public ExtensionsFilter(String... extensions) {
      this.extensions = new String[extensions.length];
      for (int i = 0; i < extensions.length; i++) {
        this.extensions[i] = extensions[i].toUpperCase();
      }
    }

    public boolean accept(File dir, String name) {
      File file = new File(dir, name);
      if (file.isDirectory()) {
        return true;
      }
      String uppercaseName = name.toUpperCase();
      for (String extension : extensions) {
        if (uppercaseName.endsWith(extension)) {
          return true;
        }
      }
      return false;
    }
  }
}
