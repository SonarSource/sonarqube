/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.measure.index;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.GT;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.LT;

public class ProjectMeasuresIndexTextSearchTest {

  private static final String NCLOC = "ncloc";

  private static final OrganizationDto ORG = OrganizationTesting.newOrganizationDto();

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private ProjectMeasuresIndexer projectMeasureIndexer = new ProjectMeasuresIndexer(null, es.client());
  private PermissionIndexerTester authorizationIndexer = new PermissionIndexerTester(es, projectMeasureIndexer);
  private ProjectMeasuresIndex underTest = new ProjectMeasuresIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);

  @Test
  public void match_exact_case_insensitive_name() {
    index(
      newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("Apache Struts")),
      newDoc(newPrivateProjectDto(ORG).setUuid("sonarqube").setName("SonarQube")));

    assertTextQueryResults("Apache Struts", "struts");
    assertTextQueryResults("APACHE STRUTS", "struts");
    assertTextQueryResults("APACHE struTS", "struts");
  }

  @Test
  public void match_from_sub_name() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("Apache Struts")));

    assertTextQueryResults("truts", "struts");
    assertTextQueryResults("pache", "struts");
    assertTextQueryResults("apach", "struts");
    assertTextQueryResults("che stru", "struts");
  }

  @Test
  public void match_name_with_dot() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("Apache.Struts")));

    assertTextQueryResults("apache struts", "struts");
  }

  @Test
  public void match_partial_name() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("XstrutsxXjavax")));

    assertTextQueryResults("struts java", "struts");
  }

  @Test
  public void match_partial_name_prefix_word1() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("MyStruts.java")));

    assertTextQueryResults("struts java", "struts");
  }

  @Test
  public void match_partial_name_suffix_word1() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("StrutsObject.java")));

    assertTextQueryResults("struts java", "struts");
  }

  @Test
  public void match_partial_name_prefix_word2() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("MyStruts.xjava")));

    assertTextQueryResults("struts java", "struts");
  }

  @Test
  public void match_partial_name_suffix_word2() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("MyStrutsObject.xjavax")));

    assertTextQueryResults("struts java", "struts");
  }

  @Test
  public void match_subset_of_document_terms() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("Some.Struts.Project.java.old")));

    assertTextQueryResults("struts java", "struts");
  }

  @Test
  public void match_partial_match_prefix_and_suffix_everywhere() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("MyStruts.javax")));

    assertTextQueryResults("struts java", "struts");
  }

  @Test
  public void ignore_empty_words() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("Struts")));

    assertTextQueryResults("            struts   \n     \n\n", "struts");
  }

  @Test
  public void match_name_from_prefix() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("Apache Struts")));

    assertTextQueryResults("apach", "struts");
    assertTextQueryResults("ApA", "struts");
    assertTextQueryResults("AP", "struts");
  }

  @Test
  public void match_name_from_two_words() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("project").setName("ApacheStrutsFoundation")));

    assertTextQueryResults("apache struts", "project");
    assertTextQueryResults("struts apache", "project");
    // Only one word is matching
    assertNoResults("apache plugin");
    assertNoResults("project struts");
  }

  @Test
  public void match_long_name() {
    index(
      newDoc(newPrivateProjectDto(ORG).setUuid("project1").setName("LongNameLongNameLongNameLongNameSonarQube")),
      newDoc(newPrivateProjectDto(ORG).setUuid("project2").setName("LongNameLongNameLongNameLongNameSonarQubeX")));

    assertTextQueryResults("LongNameLongNameLongNameLongNameSonarQube", "project1", "project2");
  }

  @Test
  public void match_name_with_two_characters() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("struts").setName("Apache Struts")));

    assertTextQueryResults("st", "struts");
    assertTextQueryResults("tr", "struts");
  }

  @Test
  public void match_exact_case_insensitive_key() {
    index(
      newDoc(newPrivateProjectDto(ORG).setUuid("project1").setName("Windows").setDbKey("project1")),
      newDoc(newPrivateProjectDto(ORG).setUuid("project2").setName("apachee").setDbKey("project2")));

    assertTextQueryResults("project1", "project1");
    assertTextQueryResults("PROJECT1", "project1");
    assertTextQueryResults("pRoJecT1", "project1");
  }

  @Test
  public void match_key_with_dot() {
    index(
      newDoc(newPrivateProjectDto(ORG).setUuid("sonarqube").setName("SonarQube").setDbKey("org.sonarqube")),
      newDoc(newPrivateProjectDto(ORG).setUuid("sq").setName("SQ").setDbKey("sonarqube")));

    assertTextQueryResults("org.sonarqube", "sonarqube");
    assertNoResults("orgsonarqube");
    assertNoResults("org-sonarqube");
    assertNoResults("org:sonarqube");
    assertNoResults("org sonarqube");
  }

  @Test
  public void match_key_with_dash() {
    index(
      newDoc(newPrivateProjectDto(ORG).setUuid("sonarqube").setName("SonarQube").setDbKey("org-sonarqube")),
      newDoc(newPrivateProjectDto(ORG).setUuid("sq").setName("SQ").setDbKey("sonarqube")));

    assertTextQueryResults("org-sonarqube", "sonarqube");
    assertNoResults("orgsonarqube");
    assertNoResults("org.sonarqube");
    assertNoResults("org:sonarqube");
    assertNoResults("org sonarqube");
  }

  @Test
  public void match_key_with_colon() {
    index(
      newDoc(newPrivateProjectDto(ORG).setUuid("sonarqube").setName("SonarQube").setDbKey("org:sonarqube")),
      newDoc(newPrivateProjectDto(ORG).setUuid("sq").setName("SQ").setDbKey("sonarqube")));

    assertTextQueryResults("org:sonarqube", "sonarqube");
    assertNoResults("orgsonarqube");
    assertNoResults("org-sonarqube");
    assertNoResults("org_sonarqube");
    assertNoResults("org sonarqube");
  }

  @Test
  public void match_key_having_all_special_characters() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("sonarqube").setName("SonarQube").setDbKey("org.sonarqube:sonar-sérvèr_ç")));

    assertTextQueryResults("org.sonarqube:sonar-sérvèr_ç", "sonarqube");
  }

  @Test
  public void does_not_match_partial_key() {
    index(newDoc(newPrivateProjectDto(ORG).setUuid("project").setName("some name").setDbKey("theKey")));

    assertNoResults("theke");
    assertNoResults("hekey");
  }

  @Test
  public void facets_take_into_account_text_search() {
    index(
      // docs with ncloc<1K
      newDoc(newPrivateProjectDto(ORG).setName("Windows").setDbKey("project1"), NCLOC, 0d),
      newDoc(newPrivateProjectDto(ORG).setName("apachee").setDbKey("project2"), NCLOC, 999d),
      // docs with ncloc>=1K and ncloc<10K
      newDoc(newPrivateProjectDto(ORG).setName("Apache").setDbKey("project3"), NCLOC, 1_000d),
      // docs with ncloc>=100K and ncloc<500K
      newDoc(newPrivateProjectDto(ORG).setName("Apache Foundation").setDbKey("project4"), NCLOC, 100_000d));

    assertNclocFacet(new ProjectMeasuresQuery().setQueryText("apache"), 1L, 1L, 0L, 1L, 0L);
    assertNclocFacet(new ProjectMeasuresQuery().setQueryText("PAch"), 1L, 1L, 0L, 1L, 0L);
    assertNclocFacet(new ProjectMeasuresQuery().setQueryText("apache foundation"), 0L, 0L, 0L, 1L, 0L);
    assertNclocFacet(new ProjectMeasuresQuery().setQueryText("project3"), 0L, 1L, 0L, 0L, 0L);
    assertNclocFacet(new ProjectMeasuresQuery().setQueryText("project"), 0L, 0L, 0L, 0L, 0L);
  }

  @Test
  public void filter_by_metric_take_into_account_text_search() {
    index(
      newDoc(newPrivateProjectDto(ORG).setUuid("project1").setName("Windows").setDbKey("project1"), NCLOC, 30_000d),
      newDoc(newPrivateProjectDto(ORG).setUuid("project2").setName("apachee").setDbKey("project2"), NCLOC, 40_000d),
      newDoc(newPrivateProjectDto(ORG).setUuid("project3").setName("Apache").setDbKey("project3"), NCLOC, 50_000d),
      newDoc(newPrivateProjectDto(ORG).setUuid("project4").setName("Apache").setDbKey("project4"), NCLOC, 60_000d));

    assertResults(new ProjectMeasuresQuery().setQueryText("apache").addMetricCriterion(MetricCriterion.create(NCLOC, GT, 20_000d)), "project3", "project4", "project2");
    assertResults(new ProjectMeasuresQuery().setQueryText("apache").addMetricCriterion(MetricCriterion.create(NCLOC, LT, 55_000d)), "project3", "project2");
    assertResults(new ProjectMeasuresQuery().setQueryText("PAC").addMetricCriterion(MetricCriterion.create(NCLOC, LT, 55_000d)), "project3", "project2");
    assertResults(new ProjectMeasuresQuery().setQueryText("apachee").addMetricCriterion(MetricCriterion.create(NCLOC, GT, 30_000d)), "project2");
    assertResults(new ProjectMeasuresQuery().setQueryText("unknown").addMetricCriterion(MetricCriterion.create(NCLOC, GT, 20_000d)));
  }

  private void index(ProjectMeasuresDoc... docs) {
    es.putDocuments(TYPE_PROJECT_MEASURES, docs);
    authorizationIndexer.allow(stream(docs).map(doc -> new IndexPermissions(doc.getId(), PROJECT).allowAnyone()).collect(toList()));
  }

  private static ProjectMeasuresDoc newDoc(ComponentDto project) {
    return new ProjectMeasuresDoc()
      .setOrganizationUuid(project.getOrganizationUuid())
      .setId(project.uuid())
      .setKey(project.getDbKey())
      .setName(project.name());
  }

  private static ProjectMeasuresDoc newDoc(ComponentDto project, String metric1, Object value1) {
    return newDoc(project).setMeasures(newArrayList(newMeasure(metric1, value1)));
  }

  private static Map<String, Object> newMeasure(String key, Object value) {
    return ImmutableMap.of("key", key, "value", value);
  }

  private void assertResults(ProjectMeasuresQuery query, String... expectedProjectUuids) {
    List<String> result = underTest.search(query, new SearchOptions()).getIds();
    assertThat(result).containsExactly(expectedProjectUuids);
  }

  private void assertTextQueryResults(String queryText, String... expectedProjectUuids) {
    assertResults(new ProjectMeasuresQuery().setQueryText(queryText), expectedProjectUuids);
  }

  private void assertNoResults(String queryText) {
    assertTextQueryResults(queryText);
  }

  private void assertNclocFacet(ProjectMeasuresQuery query, Long... facetExpectedValues) {
    checkArgument(facetExpectedValues.length == 5, "5 facet values is required");
    Facets facets = underTest.search(query, new SearchOptions().addFacets(NCLOC)).getFacets();
    assertThat(facets.get(NCLOC)).containsExactly(
      entry("*-1000.0", facetExpectedValues[0]),
      entry("1000.0-10000.0", facetExpectedValues[1]),
      entry("10000.0-100000.0", facetExpectedValues[2]),
      entry("100000.0-500000.0", facetExpectedValues[3]),
      entry("500000.0-*", facetExpectedValues[4]));
  }
}
