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
package org.sonar.server.rule.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRuleCompleterTest {

  @Rule
  public DbTester dbTester = DbTester.create();

  @Test
  public void test_completeShow() {
    ActiveRuleCompleter underTest = new ActiveRuleCompleter(dbTester.getDbClient(), new Languages());
    RuleDto rule = dbTester.rules().insert();
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert();
    ActiveRuleDto activeRule = dbTester.qualityProfiles().activateRule(qualityProfile, rule);

    List<Rules.Active> result = underTest.completeShow(dbTester.getSession(), rule);

    assertThat(result).extracting(Rules.Active::getQProfile).containsExactlyInAnyOrder(qualityProfile.getKee());
    assertThat(result).extracting(Rules.Active::getSeverity).containsExactlyInAnyOrder(activeRule.getSeverityString());
  }
}
