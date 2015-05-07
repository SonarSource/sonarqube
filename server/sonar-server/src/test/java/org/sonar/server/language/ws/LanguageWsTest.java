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
package org.sonar.server.language.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Controller;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LanguageWsTest {

  private static final String CONTROLLER_LANGUAGES = "api/languages";
  private static final String ACTION_LIST = "list";

  @Mock
  private Languages languages;

  WsTester tester;

  @Before
  public void setUp() {
    Mockito.when(languages.all()).thenReturn(new Language[] {
      new Ook(),
      new LolCode(),
      new Whitespace(),
      new ArnoldC()
    });
    tester = new WsTester(new LanguageWs(new ListAction(languages)));
  }

  @Test
  public void should_be_well_defined() {
    Controller controller = tester.controller(CONTROLLER_LANGUAGES);
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.isInternal()).isFalse();
    assertThat(controller.path()).isEqualTo(CONTROLLER_LANGUAGES);
    assertThat(controller.since()).isEqualTo("5.1");
    assertThat(controller.actions()).hasSize(1);

    Action list = controller.action(ACTION_LIST);
    assertThat(list).isNotNull();
    assertThat(list.description()).isNotEmpty();
    assertThat(list.handler()).isInstanceOf(ListAction.class);
    assertThat(list.isInternal()).isFalse();
    assertThat(list.isPost()).isFalse();
    assertThat(list.responseExampleAsString()).isNotEmpty();
    assertThat(list.params()).hasSize(2);
  }

  @Test
  public void should_list_languages() throws Exception {
    tester.newGetRequest(CONTROLLER_LANGUAGES, ACTION_LIST).execute().assertJson(this.getClass(), "list.json");

    tester.newGetRequest(CONTROLLER_LANGUAGES, ACTION_LIST)
      .setParam("ps", "2")
      .execute().assertJson(this.getClass(), "list_limited.json");
    tester.newGetRequest(CONTROLLER_LANGUAGES, ACTION_LIST)
      .setParam("ps", "4")
      .execute().assertJson(this.getClass(), "list.json");
    tester.newGetRequest(CONTROLLER_LANGUAGES, ACTION_LIST)
      .setParam("ps", "10")
      .execute().assertJson(this.getClass(), "list.json");

    tester.newGetRequest(CONTROLLER_LANGUAGES, ACTION_LIST)
      .setParam("q", "ws")
      .execute().assertJson(this.getClass(), "list_filtered_key.json");
    tester.newGetRequest(CONTROLLER_LANGUAGES, ACTION_LIST)
      .setParam("q", "o")
      .execute().assertJson(this.getClass(), "list_filtered_name.json");
  }

  static abstract class TestLanguage extends AbstractLanguage {
    public TestLanguage(String key, String language) {
      super(key, language);
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[0];
    }
  }

  static class Ook extends TestLanguage {
    public Ook() {
      super("ook", "Ook!");
    }
  }

  static class LolCode extends TestLanguage {
    public LolCode() {
      super("lol", "LOLCODE");
    }
  }

  static class Whitespace extends TestLanguage {
    public Whitespace() {
      super("ws", "Whitespace");
    }
  }

  static class ArnoldC extends TestLanguage {
    public ArnoldC() {
      super("ac", "ArnoldC");
    }
  }
}
