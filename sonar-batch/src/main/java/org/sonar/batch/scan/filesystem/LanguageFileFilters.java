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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Languages;

public class LanguageFileFilters implements BatchComponent {
  private final Languages languages;

  public LanguageFileFilters(Languages languages) {
    this.languages = languages;
  }

  public IOFileFilter forLang(String lang) {
    String[] suffixes = languages.getSuffixes(lang);
    if (suffixes != null && suffixes.length>0) {
      return new SuffixFileFilter(suffixes, IOCase.SENSITIVE);
    }
    return TrueFileFilter.TRUE;
  }
}
