/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.code.CodeCharacteristic;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.rule.RuleDto;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class RuleMapperTest {

  @Test
  @UseDataProvider("pluginApiEnumsMappedToProtobufEnums")
  public void toWsRule_shouldMapCharacteristicEnumToProtobuf(CodeCharacteristic pluginApiEnum, Common.RuleCharacteristic protoEnum) {
    RuleMapper ruleMapper = new RuleMapper(mock(Languages.class), mock(), mock());

    RuleDto ruleDto = new RuleDto();
    ruleDto.setRuleKey(RuleKey.of("repo", "key"));
    ruleDto.setScope(RuleDto.Scope.ALL);
    ruleDto.setCharacteristic(pluginApiEnum);
    Rules.Rule rule = ruleMapper.toWsRule(ruleDto, new SearchAction.SearchResult(), Set.of());

    assertThat(rule.getCharacteristic()).isEqualTo(protoEnum);
  }

  @DataProvider
  public static Object[][] pluginApiEnumsMappedToProtobufEnums() {
    return new Object[][] {
      {CodeCharacteristic.CLEAR, Common.RuleCharacteristic.CLEAR},
      {CodeCharacteristic.TESTED, Common.RuleCharacteristic.TESTED},
      {CodeCharacteristic.ROBUST, Common.RuleCharacteristic.ROBUST},
      {CodeCharacteristic.SECURE, Common.RuleCharacteristic.SECURE},
      {CodeCharacteristic.CONSISTENT, Common.RuleCharacteristic.CONSISTENT},
      {CodeCharacteristic.COMPLIANT, Common.RuleCharacteristic.COMPLIANT},
      {CodeCharacteristic.STRUCTURED, Common.RuleCharacteristic.STRUCTURED},
      {CodeCharacteristic.PORTABLE, Common.RuleCharacteristic.PORTABLE}
    };
  }
}