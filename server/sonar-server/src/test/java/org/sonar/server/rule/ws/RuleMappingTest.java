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
package org.sonar.server.rule.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.ws.SearchOptions;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.text.MacroInterpreter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RuleMappingTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  Languages languages = new Languages();
  MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  DebtModel debtModel = mock(DebtModel.class);

  @Test
  public void toQueryOptions_load_all_fields() {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel, userSessionRule);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "1");
    request.setParam(Param.PAGE_SIZE, "10");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).isEmpty();
  }

  @Test
  public void toQueryOptions_load_only_few_simple_fields() {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel, userSessionRule);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "1");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "repo,name,lang");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.REPOSITORY.field(),
      RuleNormalizer.RuleField.NAME.field(),
      RuleNormalizer.RuleField.LANGUAGE.field());
  }

  @Test
  public void toQueryOptions_langName_requires_lang() {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel, userSessionRule);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "1");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "langName");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(RuleNormalizer.RuleField.LANGUAGE.field());
  }

  @Test
  public void toQueryOptions_debt_requires_group_of_fields() {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel, userSessionRule);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "1");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "debtRemFn");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.field(),
      RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.field(),
      RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.field());
  }

  @Test
  public void toQueryOptions_html_note_requires_markdown_note() {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel, userSessionRule);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "1");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "htmlNote");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(RuleNormalizer.RuleField.NOTE.field());
  }

  @Test
  public void toQueryOptions_debt_characteristics() {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel, userSessionRule);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "1");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "debtChar");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.CHARACTERISTIC.field());
  }

  @Test
  public void toQueryOptions_debt_overloaded() {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel, userSessionRule);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "1");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "debtOverloaded");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.CHARACTERISTIC_OVERLOADED.field(),
      RuleNormalizer.RuleField.SUB_CHARACTERISTIC_OVERLOADED.field(),
      RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE_OVERLOADED.field());
  }
}
