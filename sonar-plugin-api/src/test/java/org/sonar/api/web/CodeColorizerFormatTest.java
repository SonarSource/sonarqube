/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.web;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sonar.colorizer.Tokenizer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CodeColorizerFormatTest {

  @Test
  public void keyIsLanguage() {
    CodeColorizerFormat format = new FakeFormat("foo");
    assertThat(format.getLanguageKey(), is("foo"));

    assertThat(format.equals(new FakeFormat("foo")), is(true));
    assertThat(format.equals(new FakeFormat("bar")), is(false));
  }

  private static class FakeFormat extends CodeColorizerFormat {

    public FakeFormat(String languageKey) {
      super(languageKey);
    }

    @Override
    public List<Tokenizer> getTokenizers() {
      return Collections.emptyList();
    }
  }
}
