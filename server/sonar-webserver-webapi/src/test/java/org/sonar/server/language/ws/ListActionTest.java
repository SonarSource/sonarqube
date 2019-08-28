/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.language.ws;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Mockito.mock;

public class ListActionTest {
  private static final String EMPTY_JSON_RESPONSE = "{\"languages\": []}";

  private Languages languages = mock(Languages.class);
  private ListAction underTest = new ListAction(languages);
  private WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    Mockito.when(languages.all()).thenReturn(new Language[] {
      new Ook(),
      new LolCode(),
      new Whitespace(),
      new ArnoldC()
    });
  }

  @Test
  public void list_all_languages() {
    tester.newRequest().execute().assertJson(this.getClass(), "list.json");

    tester.newRequest()
      .setParam("ps", "2")
      .execute().assertJson(this.getClass(), "list_limited.json");
    tester.newRequest()
      .setParam("ps", "4")
      .execute().assertJson(this.getClass(), "list.json");
    tester.newRequest()
      .setParam("ps", "10")
      .execute().assertJson(this.getClass(), "list.json");
  }

  @Test
  public void filter_languages_by_key_or_name() {
    tester.newRequest()
      .setParam("q", "ws")
      .execute().assertJson(this.getClass(), "list_filtered_key.json");
    tester.newRequest()
      .setParam("q", "o")
      .execute().assertJson(this.getClass(), "list_filtered_name.json");
  }

  /**
   * Potential vulnerability : the query provided by user must
   * not be executed as a regexp.
   */
  @Test
  public void filter_escapes_the_user_query() {
    // invalid regexp
    tester.newRequest()
      .setParam("q", "[")
      .execute().assertJson(EMPTY_JSON_RESPONSE);

    // do not consider param as a regexp
    tester.newRequest()
      .setParam("q", ".*")
      .execute().assertJson(EMPTY_JSON_RESPONSE);
  }

  static abstract class TestLanguage extends AbstractLanguage {
    TestLanguage(String key, String language) {
      super(key, language);
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[0];
    }
  }

  static class Ook extends TestLanguage {
    Ook() {
      super("ook", "Ook!");
    }
  }

  static class LolCode extends TestLanguage {
    LolCode() {
      super("lol", "LOLCODE");
    }
  }

  static class Whitespace extends TestLanguage {
    Whitespace() {
      super("ws", "Whitespace");
    }
  }

  static class ArnoldC extends TestLanguage {
    ArnoldC() {
      super("ac", "ArnoldC");
    }
  }
}
