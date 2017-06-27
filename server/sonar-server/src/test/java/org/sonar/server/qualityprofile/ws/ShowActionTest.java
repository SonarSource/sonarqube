/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.api.resources.Language;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.ws.WsActionTester;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.test.JsonAssert.assertJson;

public class ShowActionTest {

  @Rule
  public DbTester db = DbTester.create();

  private WsActionTester ws = new WsActionTester(new ShowAction());

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("show");
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.since()).isEqualTo("6.5");

    WebService.Param profile = action.param("profile");
    assertThat(profile).isNotNull();
    assertThat(profile.isRequired()).isTrue();
    assertThat(profile.isInternal()).isFalse();
    assertThat(profile.description()).isNotEmpty();

    WebService.Param compareToSonarWay = action.param("compareToSonarWay");
    assertThat(compareToSonarWay).isNotNull();
    assertThat(compareToSonarWay.isRequired()).isFalse();
    assertThat(compareToSonarWay.isInternal()).isTrue();
    assertThat(compareToSonarWay.description()).isNotEmpty();
    assertThat(compareToSonarWay.possibleValues()).contains("true", "false");
  }

  @Test
  public void test_example() {
    Language cs = newLanguage("cs", "C#");
    QProfileDto parentProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setKee("AU-TpxcA-iU5OvuD2FL1").setName("Parent Company Profile"));
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p
      .setKee("AU-TpxcA-iU5OvuD2FL3")
      .setName("My Company Profile")
      .setLanguage(cs.getKey())
      .setIsBuiltIn(false)
      .setRulesUpdatedAt("2016-12-22T19:10:03+0100")
      .setParentKee(parentProfile.getKee()));
    // Active rules
    range(0, 10)
      .mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(cs.getKey())).getDefinition())
      .forEach(r -> db.qualityProfiles().activateRule(profile, r));
    // Projects
    range(0, 7)
      .mapToObj(i -> db.components().insertPrivateProject())
      .forEach(project -> db.qualityProfiles().associateWithProject(project, profile));

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }
}
