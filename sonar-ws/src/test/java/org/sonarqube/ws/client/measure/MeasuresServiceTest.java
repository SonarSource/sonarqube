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
package org.sonarqube.ws.client.measure;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.ComponentTreeWsResponse;
import org.sonarqube.ws.WsMeasures.ComponentWsResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.DEPRECATED_PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.DEPRECATED_PARAM_BASE_COMPONENT_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.DEPRECATED_PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.DEPRECATED_PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_DEVELOPER_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRICS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_SORT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_SORT_FILTER;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_PROJECT_KEYS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_STRATEGY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_TO;

public class MeasuresServiceTest {
  private static final String VALUE_BASE_COMPONENT_ID = "base-component-id";
  private static final String VALUE_BASE_COMPONENT_KEY = "base-component-key";
  private static final String VALUE_COMPONENT = "component-key";
  private static final List<String> VALUE_METRIC_KEYS = newArrayList("ncloc", "complexity");
  private static final List<String> VALUE_METRICS = newArrayList("ncloc", "complexity");
  private static final String VALUE_STRATEGY = "all";
  private static final List<String> VALUE_QUALIFIERS = newArrayList("FIL", "PRJ");
  private static final ArrayList<String> VALUE_ADDITIONAL_FIELDS = newArrayList("metrics");
  private static final List<String> VALUE_SORT = newArrayList("qualifier", "metric");
  private static final boolean VALUE_ASC = false;
  private static final String VALUE_METRIC_SORT = "ncloc";
  private static final String VALUE_METRIC_SORT_FILTER = "all";
  private static final int VALUE_PAGE = 42;
  private static final int VALUE_PAGE_SIZE = 1000;
  private static final String VALUE_QUERY = "query-sq";
  private static final String VALUE_DEVELOPER_ID = "developer-id";
  private static final String VALUE_DEVELOPER_KEY = "developer-key";
  private static final String VALUE_FROM = "2017-10-01";
  private static final String VALUE_TO = "2017-11-01";

  @Rule
  public ServiceTester<MeasuresService> serviceTester = new ServiceTester<>(new MeasuresService(mock(WsConnector.class)));

  private MeasuresService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void component() {
    ComponentWsRequest request = new ComponentWsRequest()
      .setComponentId(VALUE_BASE_COMPONENT_ID)
      .setComponentKey(VALUE_BASE_COMPONENT_KEY)
      .setComponent(VALUE_BASE_COMPONENT_KEY)
      .setBranch("my_branch")
      .setMetricKeys(VALUE_METRIC_KEYS)
      .setAdditionalFields(VALUE_ADDITIONAL_FIELDS)
      .setMetricKeys(VALUE_METRICS)
      .setDeveloperId(VALUE_DEVELOPER_ID)
      .setDeveloperKey(VALUE_DEVELOPER_KEY);

    underTest.component(request);
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ComponentWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(DEPRECATED_PARAM_COMPONENT_ID, VALUE_BASE_COMPONENT_ID)
      .hasParam(DEPRECATED_PARAM_COMPONENT_KEY, VALUE_BASE_COMPONENT_KEY)
      .hasParam(PARAM_COMPONENT, VALUE_BASE_COMPONENT_KEY)
      .hasParam(PARAM_BRANCH, "my_branch")
      .hasParam(PARAM_METRIC_KEYS, "ncloc,complexity")
      .hasParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .hasParam(PARAM_DEVELOPER_ID, VALUE_DEVELOPER_ID)
      .hasParam(PARAM_DEVELOPER_KEY, VALUE_DEVELOPER_KEY)
      .andNoOtherParam();
  }

  @Test
  public void component_tree() {
    ComponentTreeWsRequest componentTreeRequest = new ComponentTreeWsRequest()
      .setBaseComponentId(VALUE_BASE_COMPONENT_ID)
      .setBaseComponentKey(VALUE_BASE_COMPONENT_KEY)
      .setComponent(VALUE_BASE_COMPONENT_KEY)
      .setMetricKeys(VALUE_METRIC_KEYS)
      .setStrategy(VALUE_STRATEGY)
      .setQualifiers(VALUE_QUALIFIERS)
      .setAdditionalFields(VALUE_ADDITIONAL_FIELDS)
      .setSort(VALUE_SORT)
      .setAsc(VALUE_ASC)
      .setMetricSort(VALUE_METRIC_SORT)
      .setPage(VALUE_PAGE)
      .setPageSize(VALUE_PAGE_SIZE)
      .setQuery(VALUE_QUERY)
      .setDeveloperId(VALUE_DEVELOPER_ID)
      .setDeveloperKey(VALUE_DEVELOPER_KEY)
      .setMetricSortFilter(VALUE_METRIC_SORT_FILTER);

    underTest.componentTree(componentTreeRequest);
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ComponentTreeWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(DEPRECATED_PARAM_BASE_COMPONENT_ID, VALUE_BASE_COMPONENT_ID)
      .hasParam(DEPRECATED_PARAM_BASE_COMPONENT_KEY, VALUE_BASE_COMPONENT_KEY)
      .hasParam(PARAM_COMPONENT, VALUE_BASE_COMPONENT_KEY)
      .hasParam(PARAM_METRIC_KEYS, "ncloc,complexity")
      .hasParam(PARAM_STRATEGY, VALUE_STRATEGY)
      .hasParam(PARAM_QUALIFIERS, "FIL,PRJ")
      .hasParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .hasParam("s", "qualifier,metric")
      .hasParam("asc", VALUE_ASC)
      .hasParam(PARAM_METRIC_SORT, VALUE_METRIC_SORT)
      .hasParam("p", VALUE_PAGE)
      .hasParam("ps", VALUE_PAGE_SIZE)
      .hasParam("q", VALUE_QUERY)
      .hasParam(PARAM_DEVELOPER_ID, VALUE_DEVELOPER_ID)
      .hasParam(PARAM_DEVELOPER_KEY, VALUE_DEVELOPER_KEY)
      .hasParam(PARAM_METRIC_SORT_FILTER, VALUE_METRIC_SORT_FILTER)
      .andNoOtherParam();
  }

  @Test
  public void search_history() {
    SearchHistoryRequest request = SearchHistoryRequest.builder()
      .setComponent(VALUE_COMPONENT)
      .setBranch("my_branch")
      .setMetrics(VALUE_METRICS)
      .setFrom(VALUE_FROM)
      .setTo(VALUE_TO)
      .setPage(VALUE_PAGE)
      .setPageSize(VALUE_PAGE_SIZE)
      .build();

    underTest.searchHistory(request);
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(WsMeasures.SearchHistoryResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_COMPONENT, VALUE_COMPONENT)
      .hasParam(PARAM_BRANCH, "my_branch")
      .hasParam(PARAM_METRICS, "ncloc,complexity")
      .hasParam(PARAM_FROM, VALUE_FROM)
      .hasParam(PARAM_TO, VALUE_TO)
      .hasParam("p", VALUE_PAGE)
      .hasParam("ps", VALUE_PAGE_SIZE)
      .andNoOtherParam();
  }

  @Test
  public void search() {
    SearchRequest request = SearchRequest.builder()
      .setProjectKeys(asList("P1", "P2"))
      .setMetricKeys(asList("ncloc", "complexity"))
      .build();

    underTest.search(request);
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(WsMeasures.SearchWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_PROJECT_KEYS, "P1,P2")
      .hasParam(PARAM_METRIC_KEYS, "ncloc,complexity")
      .andNoOtherParam();
  }
}
