/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Languages;

import java.lang.reflect.Field;

import static org.fest.assertions.Assertions.assertThat;

public class LanguageFiltersTest {
  @Test
  public void forLang() throws Exception {
    LanguageFilters filters = new LanguageFilters(new Languages(new Java(), new Php()));

    IOFileFilter filter = filters.forLang("php");
    assertThat(filter).isInstanceOf(SuffixFileFilter.class);
    assertThat(suffixes((SuffixFileFilter) filter)).containsOnly("php");

    filter = filters.forLang("java");
    assertThat(filter).isInstanceOf(SuffixFileFilter.class);
    assertThat(suffixes((SuffixFileFilter) filter)).containsOnly("java", "jav");

    assertThat(filters.forLang("unknown")).isSameAs(FalseFileFilter.FALSE);
  }

  private String[] suffixes(SuffixFileFilter filter) throws Exception {
    Field privateField = SuffixFileFilter.class.getDeclaredField("suffixes");
    privateField.setAccessible(true);

    return (String[]) privateField.get(filter);
  }

  static class Php extends AbstractLanguage {
    public Php() {
      super("php");
    }

    public String[] getFileSuffixes() {
      return new String[]{"php"};
    }
  }

  static class Java extends AbstractLanguage {
    public Java() {
      super("java");
    }

    public String[] getFileSuffixes() {
      return new String[]{"java", "jav"};
    }
  }
}
