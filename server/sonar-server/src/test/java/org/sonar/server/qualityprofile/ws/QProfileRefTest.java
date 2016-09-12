/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.qualityprofile.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.server.qualityprofile.QProfileRef;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;


public class QProfileRefTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_ref_by_key() {
    QProfileRef ref = QProfileRef.fromKey("foo");
    assertThat(ref.hasKey()).isTrue();
    assertThat(ref.getKey()).isEqualTo("foo");
  }

  @Test
  public void getLanguage_throws_ISE_if_key_ref() {
    QProfileRef ref = QProfileRef.fromKey("foo");

    expectedException.expect(IllegalStateException.class);
    ref.getLanguage();
  }

  @Test
  public void getName_throws_ISE_if_key_ref() {
    QProfileRef ref = QProfileRef.fromKey("foo");

    expectedException.expect(IllegalStateException.class);
    ref.getName();
  }

  @Test
  public void create_ref_by_name() {
    QProfileRef ref = QProfileRef.fromName("js", "Sonar way");
    assertThat(ref.hasKey()).isFalse();
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void getKey_throws_ISE_if_name_ref() {
    QProfileRef ref = QProfileRef.fromName("js", "Sonar way");

    expectedException.expect(IllegalStateException.class);
    ref.getKey();
  }


  @Test
  public void create_key_ref_from_ws_request() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam("profileKey", "foo");

    QProfileRef ref = QProfileRef.from(req);
    assertThat(ref.getKey()).isEqualTo("foo");
  }

  @Test
  public void create_name_ref_from_ws_request() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam("language", "js");
    req.setParam("profileName", "Sonar way");

    QProfileRef ref = QProfileRef.from(req);
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void create_name_ref_throws_IAE_if_language_is_missing() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam(QProfileRef.PARAM_PROFILE_KEY, "the key");
    req.setParam(QProfileRef.PARAM_PROFILE_NAME, "the name");

    expectedException.expect(IllegalArgumentException.class);
    QProfileRef.from(req);
  }

  @Test
  public void throw_IAE_if_request_does_not_define_ref() {
    SimpleGetRequest req = new SimpleGetRequest();

    expectedException.expect(IllegalArgumentException.class);
    QProfileRef.from(req);
  }

  @Test
  public void define_ws_parameters() {
    WsTester wsTester = new WsTester();
    WebService.NewController controller = wsTester.context().createController("api/qualityprofiles");
    WebService.NewAction newAction = controller.createAction("do").setHandler((request, response) -> {
    });

    Languages languages = new Languages(new FakeLanguage("java"), new FakeLanguage("js"));
    QProfileRef.defineParams(newAction, languages);

    controller.done();
    WebService.Action action = wsTester.controller("api/qualityprofiles").action("do");
    assertThat(action.param("language")).isNotNull();
    assertThat(action.param("language").possibleValues()).containsOnly("java", "js");
    assertThat(action.param("profileKey")).isNotNull();
    assertThat(action.param("profileName")).isNotNull();
  }

  @Test
  public void test_equals_and_hashCode_of_key_ref() {
    QProfileRef key1 = QProfileRef.fromKey("one");
    QProfileRef key1bis = QProfileRef.fromKey("one");
    QProfileRef key2 = QProfileRef.fromKey("two");
    QProfileRef name = QProfileRef.fromName("js", "one");

    assertThat(key1.equals(key1)).isTrue();
    assertThat(key1.equals(key1bis)).isTrue();
    assertThat(key1.equals(key2)).isFalse();
    assertThat(key1.equals(name)).isFalse();

    assertThat(key1.hashCode()).isEqualTo(key1.hashCode());
    assertThat(key1.hashCode()).isEqualTo(key1bis.hashCode());
  }

  @Test
  public void test_equals_and_hashCode_of_name_ref() {
    QProfileRef name1 = QProfileRef.fromName("js", "one");
    QProfileRef name1bis = QProfileRef.fromName("js", "one");
    QProfileRef name2 = QProfileRef.fromName("js", "two");
    QProfileRef name1OtherLang = QProfileRef.fromName("java", "one");
    QProfileRef key = QProfileRef.fromKey("one");

    assertThat(name1.equals(name1)).isTrue();
    assertThat(name1.equals(name1bis)).isTrue();
    assertThat(name1.equals(name2)).isFalse();
    assertThat(name1.equals(name1OtherLang)).isFalse();
    assertThat(name1.equals(key)).isFalse();

    assertThat(name1.hashCode()).isEqualTo(name1.hashCode());
    assertThat(name1.hashCode()).isEqualTo(name1bis.hashCode());
  }

  private static class FakeLanguage implements Language {
    private final String key;

    public FakeLanguage(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return key;
    }

    @Override
    public String[] getFileSuffixes() {
      throw new UnsupportedOperationException();
    }
  }
}
