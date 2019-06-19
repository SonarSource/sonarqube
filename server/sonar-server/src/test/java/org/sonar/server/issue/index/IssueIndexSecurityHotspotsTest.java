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
package org.sonar.server.issue.index;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexSecurityHotspotsTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = none();
  private System2 system2 = new TestSystem2().setNow(1_500_000_000_000L).setDefaultTimeZone(getTimeZone("GMT-01:00"));
  @Rule
  public DbTester db = DbTester.create(system2);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private PermissionIndexerTester authorizationIndexer = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));

  @Test
  public void filter_by_security_hotspots_type() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setType(BUG),
      newDoc("I2", file).setType(CODE_SMELL),
      newDoc("I3", file).setType(VULNERABILITY),
      newDoc("I4", file).setType(VULNERABILITY),
      newDoc("I5", file).setType(SECURITY_HOTSPOT),
      newDoc("I6", file).setType(SECURITY_HOTSPOT));

    assertThatSearchReturnsOnly(IssueQuery.builder().types(singletonList(SECURITY_HOTSPOT.name())), "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().types(asList(SECURITY_HOTSPOT.name(), VULNERABILITY.name())), "I3", "I4", "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1", "I2", "I3", "I4", "I5", "I6");
  }

  @Test
  public void filter_by_severities_ignore_hotspots() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setSeverity(Severity.INFO).setType(BUG),
      newDoc("I2", file).setSeverity(Severity.MAJOR).setType(CODE_SMELL),
      newDoc("I3", file).setSeverity(Severity.MAJOR).setType(VULNERABILITY),
      newDoc("I4", file).setSeverity(Severity.CRITICAL).setType(VULNERABILITY),
      // This entry should be ignored
      newDoc("I5", file).setSeverity(Severity.MAJOR).setType(SECURITY_HOTSPOT));

    assertThatSearchReturnsOnly(IssueQuery.builder().severities(asList(Severity.INFO, Severity.MAJOR)), "I1", "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().severities(asList(Severity.INFO, Severity.MAJOR)).types(singletonList(VULNERABILITY.name())), "I3");
    assertThatSearchReturnsEmpty(IssueQuery.builder().severities(singletonList(Severity.MAJOR)).types(singletonList(SECURITY_HOTSPOT.name())));
  }

  @Test
  public void facet_on_severities_ignore_hotspots() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setSeverity(INFO).setType(BUG),
      newDoc("I2", file).setSeverity(INFO).setType(CODE_SMELL),
      newDoc("I3", file).setSeverity(INFO).setType(VULNERABILITY),
      newDoc("I4", file).setSeverity(MAJOR).setType(VULNERABILITY),
      // These 2 entries should be ignored
      newDoc("I5", file).setSeverity(INFO).setType(SECURITY_HOTSPOT),
      newDoc("I6", file).setSeverity(MAJOR).setType(SECURITY_HOTSPOT));

    assertThatFacetHasOnly(IssueQuery.builder(), "severities", entry("INFO", 3L), entry("MAJOR", 1L));
    assertThatFacetHasOnly(IssueQuery.builder().types(singletonList(VULNERABILITY.name())), "severities", entry("INFO", 1L), entry("MAJOR", 1L));
    assertThatFacetHasOnly(IssueQuery.builder().types(asList(BUG.name(), CODE_SMELL.name(), VULNERABILITY.name())), "severities", entry("INFO", 3L), entry("MAJOR", 1L));
    assertThatFacetHasOnly(IssueQuery.builder().types(singletonList(SECURITY_HOTSPOT.name())), "severities");
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    authorizationIndexer.allow(stream(issues).map(issue -> new IndexPermissions(issue.projectUuid(), PROJECT).allowAnyone()).collect(toList()));
  }

  @SafeVarargs
  private final void assertThatFacetHasOnly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone());
    assertThat(facets.getNames()).containsOnly(facet, "effort");
    assertThat(facets.get(facet)).containsOnly(expectedEntries);
  }

  private void assertThatSearchReturnsOnly(IssueQuery.Builder query, String... expectedIssueKeys) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).containsExactlyInAnyOrder(expectedIssueKeys);
  }

  private void assertThatSearchReturnsEmpty(IssueQuery.Builder query) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).isEmpty();
  }

  private List<String> searchAndReturnKeys(IssueQuery.Builder query) {
    return Arrays.stream(underTest.search(query.build(), new SearchOptions()).getHits().getHits())
      .map(SearchHit::getId)
      .collect(Collectors.toList());
  }

}
