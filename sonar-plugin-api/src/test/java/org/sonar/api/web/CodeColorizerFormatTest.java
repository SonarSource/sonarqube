/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.web;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sonar.colorizer.Tokenizer;

import static org.assertj.core.api.Assertions.assertThat;

public class CodeColorizerFormatTest {

  @Test
  public void keyIsLanguage() {
    CodeColorizerFormat format = new FakeFormat("foo");
    assertThat(format.getLanguageKey()).isEqualTo("foo");

    assertThat(format.equals(new FakeFormat("foo"))).isTrue();
    assertThat(format.equals(new FakeFormat("bar"))).isFalse();
    assertThat(format.hashCode()).isEqualTo(format.hashCode());
    assertThat(format.hashCode()).isEqualTo(new FakeFormat("foo").hashCode());
    assertThat(format.toString()).isEqualTo("FakeFormat{lang=foo}");
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
