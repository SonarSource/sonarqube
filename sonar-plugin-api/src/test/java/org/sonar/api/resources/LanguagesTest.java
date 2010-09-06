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
package org.sonar.api.resources;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.sonar.api.database.model.ResourceModel;

import java.io.File;
import java.util.List;

public class LanguagesTest {

  @Test
  public void shouldAddSeveralTimesTheSameLanguage() {
    FakeLanguage fake = new FakeLanguage();
    Languages languages = new Languages(fake, fake);
    assertEquals("fake", languages.get("fake").getKey());
  }


  @Test
  public void getSuffixes() {
    Languages languages = new Languages(
        newLang("java", new String[]{"java"}),
        newLang("php", new String[]{"php4", "php5"}));

    assertThat(languages.getSuffixes(), hasItemInArray("java"));
    assertThat(languages.getSuffixes(), hasItemInArray("php4"));
    assertThat(languages.getSuffixes(), hasItemInArray("php5"));

    assertArrayEquals(languages.getSuffixes("java"), new String[]{"java"});
    assertArrayEquals(languages.getSuffixes("php"), new String[]{"php4", "php5"});
    assertArrayEquals(languages.getSuffixes("xxx"), new String[0]);
  }

  private Language newLang(String key, String[] suffixes) {
    Language lang = mock(Language.class);
    when(lang.getKey()).thenReturn(key);
    when(lang.getFileSuffixes()).thenReturn(suffixes);
    return lang;
  }

  static class FakeLanguage implements Language {

    public String getKey() {
      return "fake";
    }

    public String getName() {
      return "Fake";
    }

    public String[] getFileSuffixes() {
      return new String[]{"fak"};
    }

    public ResourceModel getParent(ResourceModel resource) {
      return null;
    }

    public boolean matchExclusionPattern(ResourceModel resource, String wildcardPattern) {
      return false;
    }

    public boolean matchExclusionPattern(File source, List<File> sourceDirs, String wildcardPattern) {
      return false;
    }

  }
}
