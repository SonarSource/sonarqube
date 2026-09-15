/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.qualityprofile;

import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.sonar.api.resources.Language;

public final class VirtualLanguageEvaluator {

  /**
   * Known real languages that ship an empty default file-suffix/pattern list (e.g. COBOL's
   * {@code sonar.cobol.file.suffixes} defaults to "", by plugin design, until a project configures it),
   * which otherwise makes them indistinguishable from genuinely virtual languages like Secrets or Text.
   */
  private static final Set<String> NON_VIRTUAL_LANGUAGES_WITH_NO_DEFAULT_FILE_SUFFIX = Set.of("cobol");

  private VirtualLanguageEvaluator() {
    // utility class
  }

  public static boolean isVirtual(@Nullable Language language) {
    if (language == null || NON_VIRTUAL_LANGUAGES_WITH_NO_DEFAULT_FILE_SUFFIX.contains(language.getKey())) {
      return false;
    }
    return ArrayUtils.isEmpty(language.getFileSuffixes()) && ArrayUtils.isEmpty(language.filenamePatterns());
  }
}
