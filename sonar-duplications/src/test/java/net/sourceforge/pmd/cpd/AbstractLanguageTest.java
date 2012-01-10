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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.junit.Test;

/**
 * We use modified version of {@link AbstractLanguage} in comparison with PMD - it doesn't use package "net.sourceforge.pmd.util.filter",
 * so goal of this test is to verify that behavior wasn't changed:
 * filter should always accept directories and files with a specified set of extensions (comparison is case insensitive).
 */
public class AbstractLanguageTest {

  @Test
  public void shouldCreateCorrectFilenameFilterForExtensions() throws IOException {
    AbstractLanguage language = new AbstractLanguage(null, "java") {
    };

    FilenameFilter filter = language.getFileFilter();
    assertThat(filter.accept(new File("test-resources"), "org"), is(true));
    assertThat(filter.accept(new File("test-resources/org/sonar/duplications/cpd/CPDTest"), "CPDFile1.java"), is(true));
    assertThat(filter.accept(new File("test-resources/org/sonar/duplications/cpd/CPDTest"), "CPDFile1.cpp"), is(false));

    language = new AbstractLanguage(null, "Java") {
    };
    assertThat(filter.accept(new File("test-resources/org/sonar/duplications/cpd/CPDTest"), "CPDFile1.java"), is(true));

    language = new AbstractLanguage(null, new String[] {}) {
    };
    assertThat(filter.accept(new File("test-resources/org/sonar/duplications/cpd/CPDTest"), "CPDFile1.java"), is(true));
  }

  @Test(expected = NullPointerException.class)
  public void shouldThrowException() {
    new AbstractLanguage(null, null) {
    };
  }

}
