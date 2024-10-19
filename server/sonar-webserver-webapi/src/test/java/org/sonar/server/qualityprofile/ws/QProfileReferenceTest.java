/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.junit.Test;
import org.sonar.api.impl.ws.SimpleGetRequest;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.language.LanguageTesting.newLanguage;

public class QProfileReferenceTest {


  @Test
  public void fromKey_creates_reference_by_key() {
    QProfileReference ref = QProfileReference.fromKey("foo");
    assertThat(ref.hasKey()).isTrue();
    assertThat(ref.getKey()).isEqualTo("foo");
  }

  @Test
  public void getLanguage_throws_ISE_on_reference_by_key() {
    QProfileReference ref = QProfileReference.fromKey("foo");

    assertThatThrownBy(() -> ref.getLanguage())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Language is not defined. Please call hasKey().");
  }

  @Test
  public void getName_throws_ISE_on_reference_by_key() {
    QProfileReference ref = QProfileReference.fromKey("foo");

    assertThatThrownBy(() -> ref.getName())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Name is not defined. Please call hasKey().");
  }

  @Test
  public void fromName_creates_reference_by_name() {
    QProfileReference ref = QProfileReference.fromName("js", "Sonar way");
    assertThat(ref.hasKey()).isFalse();
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void getKey_throws_ISE_on_reference_by_name() {
    QProfileReference ref = QProfileReference.fromName("js", "Sonar way");

    assertThatThrownBy(() -> ref.getKey())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Key is not defined. Please call hasKey().");
  }

  @Test
  public void fromName_reads_request_parameters_and_creates_reference_by_name() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam("language", "js");
    req.setParam("qualityProfile", "Sonar way");

    QProfileReference ref = QProfileReference.fromName(req);
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void throw_IAE_if_request_does_not_define_ref() {
    SimpleGetRequest req = new SimpleGetRequest();

    assertThatThrownBy(() -> QProfileReference.fromName(req))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void define_ws_parameters() {
    WebService.Context context = new WebService.Context();
    WebService.NewController controller = context.createController("api/qualityprofiles");
    WebService.NewAction newAction = controller.createAction("do").setHandler((request, response) -> {
    });

    Languages languages = new Languages(newLanguage("java"), newLanguage("js"));
    QProfileReference.defineParams(newAction, languages);

    controller.done();
    WebService.Action action = context.controller("api/qualityprofiles").action("do");
    assertThat(action.param("language")).isNotNull();
    assertThat(action.param("language").possibleValues()).containsOnly("java", "js");
    assertThat(action.param("qualityProfile")).isNotNull();
  }

  @Test
  public void test_equals_and_hashCode_of_key_ref() {
    QProfileReference key1 = QProfileReference.fromKey("one");
    QProfileReference key1bis = QProfileReference.fromKey("one");
    QProfileReference key2 = QProfileReference.fromKey("two");
    QProfileReference name = QProfileReference.fromName("js", "one");

    assertThat(key1.equals(key1)).isTrue();
    assertThat(key1.equals(key1bis)).isTrue();
    assertThat(key1.equals(key2)).isFalse();
    assertThat(key1.equals(name)).isFalse();

    assertThat(key1)
      .hasSameHashCodeAs(key1)
      .hasSameHashCodeAs(key1bis);
  }

  @Test
  public void test_equals_and_hashCode_of_name_ref() {
    QProfileReference name1 = QProfileReference.fromName("js", "one");
    QProfileReference name1bis = QProfileReference.fromName("js", "one");
    QProfileReference name2 = QProfileReference.fromName("js", "two");
    QProfileReference name1OtherLang = QProfileReference.fromName("java", "one");
    QProfileReference key = QProfileReference.fromKey("one");

    assertThat(name1.equals(name1)).isTrue();
    assertThat(name1.equals(name1bis)).isTrue();
    assertThat(name1.equals(name2)).isFalse();
    assertThat(name1.equals(name1OtherLang)).isFalse();
    assertThat(name1.equals(key)).isFalse();

    assertThat(name1)
      .hasSameHashCodeAs(name1)
      .hasSameHashCodeAs(name1bis);
  }

}
