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

import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.debt.DebtModel;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.ws.SearchOptions;
import org.sonar.server.text.MacroInterpreter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RuleMappingTest {

  Languages languages = new Languages();
  MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  DebtModel debtModel = mock(DebtModel.class);

  @Test
  public void toQueryOptions_load_all_fields() throws Exception {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "1");
    request.setParam("ps", "10");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).isEmpty();
  }

  @Test
  public void toQueryOptions_load_only_few_simple_fields() throws Exception {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "1");
    request.setParam("ps", "10");
    request.setParam("f", "repo,name,lang");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.REPOSITORY.field(),
      RuleNormalizer.RuleField.NAME.field(),
      RuleNormalizer.RuleField.LANGUAGE.field());
  }

  @Test
  public void toQueryOptions_langName_requires_lang() throws Exception {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "1");
    request.setParam("ps", "10");
    request.setParam("f", "langName");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(RuleNormalizer.RuleField.LANGUAGE.field());
  }

  @Test
  public void toQueryOptions_debt_requires_group_of_fields() throws Exception {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "1");
    request.setParam("ps", "10");
    request.setParam("f", "debtRemFn");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.field(),
      RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.field(),
      RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.field());
  }

  @Test
  public void toQueryOptions_html_note_requires_markdown_note() throws Exception {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "1");
    request.setParam("ps", "10");
    request.setParam("f", "htmlNote");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(RuleNormalizer.RuleField.NOTE.field());
  }

  @Test
  public void toQueryOptions_debt_characteristics() throws Exception {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "1");
    request.setParam("ps", "10");
    request.setParam("f", "debtChar");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.CHARACTERISTIC.field());
  }

  @Test
  public void toQueryOptions_debt_overloaded() throws Exception {
    RuleMapping mapping = new RuleMapping(languages, macroInterpreter, debtModel);
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "1");
    request.setParam("ps", "10");
    request.setParam("f", "debtOverloaded");
    QueryContext queryContext = mapping.newQueryOptions(SearchOptions.create(request));

    assertThat(queryContext.getFieldsToReturn()).containsOnly(
      RuleNormalizer.RuleField.CHARACTERISTIC_OVERLOADED.field(),
      RuleNormalizer.RuleField.SUB_CHARACTERISTIC_OVERLOADED.field(),
      RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE_OVERLOADED.field());
  }
}
