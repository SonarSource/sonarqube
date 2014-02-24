/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.FilenameUtils;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

/**
 * Additional {@link org.sonar.api.batch.fs.FilePredicate}s that are
 * not published in public API
 */
class AdditionalFilePredicates {

  private AdditionalFilePredicates() {
    // only static inner classes
  }

  static class KeyPredicate implements FilePredicate {
    private final String key;

    KeyPredicate(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(InputFile f) {
      return key.equals(((DefaultInputFile) f).key());
    }
  }

  static class DeprecatedKeyPredicate implements FilePredicate {
    private final String key;

    DeprecatedKeyPredicate(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(InputFile f) {
      return key.equals(((DefaultInputFile) f).deprecatedKey());
    }
  }

  static class SourceRelativePathPredicate implements FilePredicate {
    private final String path;

    SourceRelativePathPredicate(String s) {
      this.path = FilenameUtils.normalize(s, true);
    }

    @Override
    public boolean apply(InputFile f) {
      return path.equals(((DefaultInputFile) f).pathRelativeToSourceDir());
    }
  }

  static class SourceDirPredicate implements FilePredicate {
    private final String path;

    SourceDirPredicate(String s) {
      this.path = FilenameUtils.normalize(s, true);
    }

    @Override
    public boolean apply(InputFile f) {
      return path.equals(((DefaultInputFile) f).sourceDirAbsolutePath());
    }
  }
}
