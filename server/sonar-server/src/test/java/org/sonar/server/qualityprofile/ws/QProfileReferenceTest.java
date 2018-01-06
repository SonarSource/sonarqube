/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.language.LanguageTesting.newLanguage;

public class QProfileReferenceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fromKey_creates_reference_by_key() {
    QProfileReference ref = QProfileReference.fromKey("foo");
    assertThat(ref.hasKey()).isTrue();
    assertThat(ref.getKey()).isEqualTo("foo");
  }

  @Test
  public void getLanguage_throws_ISE_on_reference_by_key() {
    QProfileReference ref = QProfileReference.fromKey("foo");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Language is not defined. Please call hasKey().");
    ref.getLanguage();
  }

  @Test
  public void getName_throws_ISE_on_reference_by_key() {
    QProfileReference ref = QProfileReference.fromKey("foo");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Name is not defined. Please call hasKey().");
    ref.getName();
  }

  @Test
  public void fromName_creates_reference_by_name_on_default_organization() {
    QProfileReference ref = QProfileReference.fromName(null, "js", "Sonar way");
    assertThat(ref.hasKey()).isFalse();
    assertThat(ref.getOrganizationKey()).isEmpty();
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void fromName_creates_reference_by_name_on_specified_organization() {
    QProfileReference ref = QProfileReference.fromName("my-org", "js", "Sonar way");
    assertThat(ref.hasKey()).isFalse();
    assertThat(ref.getOrganizationKey()).hasValue("my-org");
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void getKey_throws_ISE_on_reference_by_name() {
    QProfileReference ref = QProfileReference.fromName(null, "js", "Sonar way");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Key is not defined. Please call hasKey().");

    ref.getKey();
  }

  @Test
  public void getOrganization_throws_ISE_on_reference_by_key() {
    QProfileReference ref = QProfileReference.fromKey("foo");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization is not defined. Please call hasKey().");

    ref.getOrganizationKey();
  }

  @Test
  public void from_reads_request_parameters_and_creates_reference_by_key() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam("key", "foo");

    QProfileReference ref = QProfileReference.from(req);
    assertThat(ref.getKey()).isEqualTo("foo");
  }

  @Test
  public void from_reads_request_parameters_and_creates_reference_by_name_on_default_organization() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam("language", "js");
    req.setParam("qualityProfile", "Sonar way");

    QProfileReference ref = QProfileReference.from(req);
    assertThat(ref.getOrganizationKey()).isEmpty();
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void from_reads_request_parameters_and_creates_reference_by_name_on_specified_organization() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam("organization", "my-org");
    req.setParam("language", "js");
    req.setParam("qualityProfile", "Sonar way");

    QProfileReference ref = QProfileReference.from(req);
    assertThat(ref.getOrganizationKey()).hasValue("my-org");
    assertThat(ref.getLanguage()).isEqualTo("js");
    assertThat(ref.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void from_reads_request_parameters_and_throws_IAE_if_language_is_missing() {
    SimpleGetRequest req = new SimpleGetRequest();
    req.setParam("profileName", "the name");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("If no quality profile key is specified, language and name must be set");

    QProfileReference.from(req);
  }

  @Test
  public void throw_IAE_if_request_does_not_define_ref() {
    SimpleGetRequest req = new SimpleGetRequest();

    expectedException.expect(IllegalArgumentException.class);
    QProfileReference.from(req);
  }

  @Test
  public void define_ws_parameters() {
    WsTester wsTester = new WsTester();
    WebService.NewController controller = wsTester.context().createController("api/qualityprofiles");
    WebService.NewAction newAction = controller.createAction("do").setHandler((request, response) -> {
    });

    Languages languages = new Languages(newLanguage("java"), newLanguage("js"));
    QProfileReference.defineParams(newAction, languages);

    controller.done();
    WebService.Action action = wsTester.controller("api/qualityprofiles").action("do");
    assertThat(action.param("language")).isNotNull();
    assertThat(action.param("language").possibleValues()).containsOnly("java", "js");
    assertThat(action.param("key")).isNotNull();
    assertThat(action.param("qualityProfile")).isNotNull();
  }

  @Test
  public void test_equals_and_hashCode_of_key_ref() {
    QProfileReference key1 = QProfileReference.fromKey("one");
    QProfileReference key1bis = QProfileReference.fromKey("one");
    QProfileReference key2 = QProfileReference.fromKey("two");
    QProfileReference name = QProfileReference.fromName("my-org", "js", "one");

    assertThat(key1.equals(key1)).isTrue();
    assertThat(key1.equals(key1bis)).isTrue();
    assertThat(key1.equals(key2)).isFalse();
    assertThat(key1.equals(name)).isFalse();

    assertThat(key1.hashCode()).isEqualTo(key1.hashCode());
    assertThat(key1.hashCode()).isEqualTo(key1bis.hashCode());
  }

  @Test
  public void test_equals_and_hashCode_of_name_ref() {
    QProfileReference name1 = QProfileReference.fromName("org1", "js", "one");
    QProfileReference name1bis = QProfileReference.fromName("org1", "js", "one");
    QProfileReference name2 = QProfileReference.fromName("org1", "js", "two");
    QProfileReference name1OtherLang = QProfileReference.fromName("org1", "java", "one");
    QProfileReference key = QProfileReference.fromKey("one");

    assertThat(name1.equals(name1)).isTrue();
    assertThat(name1.equals(name1bis)).isTrue();
    assertThat(name1.equals(name2)).isFalse();
    assertThat(name1.equals(name1OtherLang)).isFalse();
    assertThat(name1.equals(key)).isFalse();

    assertThat(name1.hashCode()).isEqualTo(name1.hashCode());
    assertThat(name1.hashCode()).isEqualTo(name1bis.hashCode());
  }

}
