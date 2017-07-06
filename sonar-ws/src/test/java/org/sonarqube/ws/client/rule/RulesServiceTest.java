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
package org.sonarqube.ws.client.rule;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.SearchResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TYPES;

public class RulesServiceTest {
  static final boolean ACTIVATION_VALUE = true;
  static final List<String> ACTIVE_SEVERITIES_VALUE = Lists.newArrayList("CRITICAL", "BLOCKER");
  static final String ACTIVE_SEVERITIES_VALUE_INLINED = "CRITICAL,BLOCKER";
  static final boolean ASC_VALUE = false;
  static final String AVAILABLE_SINCE_VALUE = "2015-06-22";
  static final List<String> FIELDS_VALUE = newArrayList("repo", "name");
  static final String FIELDS_VALUE_INLINED = "repo,name";
  static final List<String> FACETS_VALUE = newArrayList("languages", "repositories");
  static final String FACETS_VALUE_INLINED = "languages,repositories";
  static final List<String> INHERITANCE_VALUE = newArrayList("INHERITED", "OVERRIDES");
  static final String INHERITANCE_VALUE_INLINED = "INHERITED,OVERRIDES";
  static final boolean IS_TEMPLATE_VALUE = true;
  static final List<String> LANGUAGES_VALUE = newArrayList("java", "js");
  static final String LANGUAGES_VALUE_INLINED = "java,js";
  static final int PAGE_VALUE = 12;
  static final int PAGE_SIZE_VALUE = 42;
  static final String QUERY_VALUE = "query-value";
  static final String QPROFILE_VALUE = "qprofile-key";
  static final List<String> REPOSITORIES_VALUE = newArrayList("findbugs", "checkstyle");
  static final String REPOSITORIES_VALUE_INLINED = "findbugs,checkstyle";
  static final String RULE_KEY_VALUE = "rule-key-value";
  static final String SORT_VALUE = "name";
  static final List<String> SEVERITIES_VALUE = newArrayList("INFO", "MINOR");
  static final String SEVERITIES_VALUE_INLINED = "INFO,MINOR";
  static final List<String> STATUSES_VALUE = newArrayList("BETA", "DEPRECATED");
  static final String STATUSES_VALUE_INLINED = "BETA,DEPRECATED";
  static final List<String> TAGS_VALUE = newArrayList("clumsy", "java8");
  static final String TAGS_VALUE_INLINED = "clumsy,java8";
  static final String TEMPLATE_KEY_VALUE = "template-key-value";
  static final List<String> TYPES_VALUE = newArrayList("CODE_SMELL", "BUG");
  static final String TYPES_VALUE_INLINED = "CODE_SMELL,BUG";

  @Rule
  public ServiceTester<RulesService> serviceTester = new ServiceTester<>(new RulesService(mock(WsConnector.class)));

  private RulesService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void test_search() {
    underTest.search(new SearchWsRequest()
      .setActivation(ACTIVATION_VALUE)
      .setActiveSeverities(ACTIVE_SEVERITIES_VALUE)
      .setAsc(ASC_VALUE)
      .setAvailableSince(AVAILABLE_SINCE_VALUE)
      .setFields(FIELDS_VALUE)
      .setFacets(FACETS_VALUE)
      .setInheritance(INHERITANCE_VALUE)
      .setIsTemplate(IS_TEMPLATE_VALUE)
      .setLanguages(LANGUAGES_VALUE)
      .setPage(PAGE_VALUE)
      .setPageSize(PAGE_SIZE_VALUE)
      .setQuery(QUERY_VALUE)
      .setQProfile(QPROFILE_VALUE)
      .setCompareToProfile("CompareTo")
      .setRepositories(REPOSITORIES_VALUE)
      .setRuleKey(RULE_KEY_VALUE)
      .setSort(SORT_VALUE)
      .setSeverities(SEVERITIES_VALUE)
      .setStatuses(STATUSES_VALUE)
      .setTags(TAGS_VALUE)
      .setTemplateKey(TEMPLATE_KEY_VALUE)
      .setTypes(TYPES_VALUE));

    assertThat(serviceTester.getGetParser()).isSameAs(SearchResponse.parser());
    GetRequest getRequest = serviceTester.getGetRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("search")
      .hasParam(PARAM_ACTIVATION, ACTIVATION_VALUE)
      .hasParam(PARAM_ACTIVE_SEVERITIES, ACTIVE_SEVERITIES_VALUE_INLINED)
      .hasParam("asc", ASC_VALUE)
      .hasParam(PARAM_AVAILABLE_SINCE, AVAILABLE_SINCE_VALUE)
      .hasParam("f", FIELDS_VALUE_INLINED)
      .hasParam("facets", FACETS_VALUE_INLINED)
      .hasParam(PARAM_INHERITANCE, INHERITANCE_VALUE_INLINED)
      .hasParam(PARAM_IS_TEMPLATE, IS_TEMPLATE_VALUE)
      .hasParam("p", PAGE_VALUE)
      .hasParam("ps", PAGE_SIZE_VALUE)
      .hasParam("q", QUERY_VALUE)
      .hasParam(PARAM_QPROFILE, QPROFILE_VALUE)
      .hasParam(PARAM_COMPARE_TO_PROFILE, "CompareTo")
      .hasParam(PARAM_REPOSITORIES, REPOSITORIES_VALUE_INLINED)
      .hasParam(PARAM_RULE_KEY, RULE_KEY_VALUE)
      .hasParam(PARAM_LANGUAGES, LANGUAGES_VALUE_INLINED)
      .hasParam("s", SORT_VALUE)
      .hasParam(PARAM_SEVERITIES, SEVERITIES_VALUE_INLINED)
      .hasParam(PARAM_STATUSES, STATUSES_VALUE_INLINED)
      .hasParam(PARAM_TAGS, TAGS_VALUE_INLINED)
      .hasParam(PARAM_TEMPLATE_KEY, TEMPLATE_KEY_VALUE)
      .hasParam(PARAM_TYPES, TYPES_VALUE_INLINED)
      .andNoOtherParam();
  }

  @Test
  public void test_show() {
    underTest.show("the-org", "the-rule/key");

    assertThat(serviceTester.getGetParser()).isSameAs(Rules.ShowResponse.parser());
    GetRequest getRequest = serviceTester.getGetRequest();
    serviceTester.assertThat(getRequest)
      .hasPath("show")
      .hasParam("organization", "the-org")
      .hasParam("key", "the-rule/key")
      .andNoOtherParam();
  }

  @Test
  public void test_create() {
    underTest.create(new CreateWsRequest.Builder()
      .setTemplateKey("the-template-key")
      .setCustomKey("the-custom-key")
      .setSeverity("BLOCKER")
      .setParams("the-params")
      .setPreventReactivation(true)
      .setMarkdownDescription("the-desc")
      .setStatus("BETA")
      .setName("the-name")
      .build());

    PostRequest postRequest = serviceTester.getPostRequest();
    serviceTester.assertThat(postRequest)
      .hasPath("create")
      .hasParam("template_key", "the-template-key")
      .hasParam("custom_key", "the-custom-key")
      .hasParam("severity", "BLOCKER")
      .hasParam("params", "the-params")
      .hasParam("prevent_reactivation", "true")
      .hasParam("markdown_description", "the-desc")
      .hasParam("status", "BETA")
      .hasParam("name", "the-name")
      .andNoOtherParam();
  }
}
