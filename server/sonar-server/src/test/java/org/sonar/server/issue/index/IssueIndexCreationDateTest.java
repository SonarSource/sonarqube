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
package org.sonar.server.issue.index;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.assertj.core.api.Fail;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerDao;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexDefinition;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexCreationDateTest {

  private MapSettings settings = new MapSettings();

  @Rule
  public EsTester es = new EsTester(
    new IssueIndexDefinition(settings.asConfig()),
    new ViewIndexDefinition(settings.asConfig()),
    new RuleIndexDefinition(settings.asConfig()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private TestSystem2 system2 = new TestSystem2()
    .setNow(DateUtils.parseDateTime("2016-07-08T09:10:11+0300").getTime())
    .setDefaultTimeZone(TimeZone.getTimeZone("GMT-1:00"));
  @Rule
  public DbTester db = DbTester.create(system2);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new AuthorizationTypeSupport(userSessionRule));

  @Test
  public void filter_by_created_after() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-19")), "I1", "I2");
    // Lower bound is included
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-20")), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-21")), "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAfter(parseDate("2014-09-25")));
  }

  @Test
  public void filter_by_created_before() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThatSearchReturnsEmpty(IssueQuery.builder().createdBefore(parseDate("2014-09-19")));
    // Upper bound is excluded
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdBefore(parseDate("2014-09-20")));
    assertThatSearchReturnsOnly(IssueQuery.builder().createdBefore(parseDate("2014-09-21")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().createdBefore(parseDate("2014-09-25")), "I1", "I2");
  }

  @Test
  public void filter_by_created_after_and_before() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", file).setFuncCreationDate(parseDate("2014-09-23")));

    // 19 < createdAt < 25
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-25")),
      "I1", "I2");

    // 20 < createdAt < 25: excludes first issue
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-25")), "I1", "I2");

    // 21 < createdAt < 25
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-25")), "I2");

    // 21 < createdAt < 24
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-24")), "I2");

    // 21 < createdAt < 23: excludes second issue
    assertThatSearchReturnsEmpty(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-23")));

    // 19 < createdAt < 21: only first issue
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-21")), "I1");

    // 20 < createdAt < 20: exception
    expectedException.expect(IllegalArgumentException.class);
    underTest.search(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-20"))
      .build(), new SearchOptions());
  }

  @Test
  public void filter_by_create_after_and_before_take_into_account_timezone() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDateTime("2014-09-20T00:00:00+0100")),
      newDoc("I2", file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDateTime("2014-09-19T23:00:00+0000")).createdBefore(parseDateTime("2014-09-22T23:00:01+0000")),
      "I1", "I2");

    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAfter(parseDateTime("2014-09-19T23:00:01+0000")).createdBefore(parseDateTime("2014-09-22T23:00:00+0000")));
  }

  @Test
  public void filter_by_created_before_must_be_lower_than_after() {
    try {
      underTest.search(IssueQuery.builder().createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-19")).build(),
        new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be larger or equal to end bound");
    }
  }

  @Test
  public void fail_if_created_before_equals_created_after() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Start bound cannot be larger or equal to end bound");

    underTest.search(IssueQuery.builder().createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-20")).build(), new SearchOptions());
  }

  @Test
  public void filter_by_created_after_must_not_be_in_future() {
    try {
      underTest.search(IssueQuery.builder().createdAfter(new Date(Long.MAX_VALUE)).build(), new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be in the future");
    }
  }

  @Test
  public void filter_by_created_at() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAt(parseDate("2014-09-20")), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAt(parseDate("2014-09-21")));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_days() {
    SearchOptions options = fixtureForCreatedAtFacet();

    IssueQuery query = IssueQuery.builder()
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2014-09-08T00:00:00+0100"))
      .checkAuthorization(false)
      .build();
    SearchResponse result = underTest.search(query, options);
    Map<String, Long> buckets = new Facets(result).get("createdAt");
    assertThat(buckets).containsOnly(
      entry("2014-08-31T01:00:00+0000", 0L),
      entry("2014-09-01T01:00:00+0000", 2L),
      entry("2014-09-02T01:00:00+0000", 1L),
      entry("2014-09-03T01:00:00+0000", 0L),
      entry("2014-09-04T01:00:00+0000", 0L),
      entry("2014-09-05T01:00:00+0000", 1L),
      entry("2014-09-06T01:00:00+0000", 0L),
      entry("2014-09-07T01:00:00+0000", 0L));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_weeks() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
        .createdBefore(parseDateTime("2014-09-21T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-25T01:00:00+0000", 0L),
      entry("2014-09-01T01:00:00+0000", 4L),
      entry("2014-09-08T01:00:00+0000", 0L),
      entry("2014-09-15T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_months() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
        .createdBefore(parseDateTime("2015-01-19T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-01T01:00:00+0000", 0L),
      entry("2014-09-01T01:00:00+0000", 5L),
      entry("2014-10-01T01:00:00+0000", 0L),
      entry("2014-11-01T01:00:00+0000", 0L),
      entry("2014-12-01T01:00:00+0000", 0L),
      entry("2015-01-01T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_with_more_than_20_months() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2011-01-01T00:00:00+0100"))
        .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2010-01-01T01:00:00+0000", 0L),
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L));

  }

  @Test
  public void facet_on_created_at_with_one_day() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdAfter(parseDateTime("2014-09-01T00:00:00-0100"))
        .createdBefore(parseDateTime("2014-09-02T00:00:00-0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-09-01T01:00:00+0000", 2L));
  }

  @Test
  public void facet_on_created_at_with_bounds_outside_of_data() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdAfter(parseDateTime("2009-01-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100"))
      .build(), options);
    Map<String, Long> createdAt = new Facets(result).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2008-01-01T01:00:00+0000", 0L),
      entry("2009-01-01T01:00:00+0000", 0L),
      entry("2010-01-01T01:00:00+0000", 0L),
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_without_start_bound() {
    SearchOptions searchOptions = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
        .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      searchOptions);
    Map<String, Long> createdAt = new Facets(result).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_without_issues() {
    SearchOptions searchOptions = new SearchOptions().addFacets("createdAt");

    SearchResponse result = underTest.search(IssueQuery.builder().build(), searchOptions);
    Map<String, Long> createdAt = new Facets(result).get("createdAt");
    assertThat(createdAt).isNull();
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    for (IssueDoc issue : issues) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(issue.projectUuid(), "TRK");
      access.allowAnyone();
      authorizationIndexerTester.allow(access);
    }
  }

  private SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    IssueDoc issue0 = newDoc("ISSUE0", file).setFuncCreationDate(parseDateTime("2011-04-25T00:05:13+0000"));
    IssueDoc issue1 = newDoc("I1", file).setFuncCreationDate(parseDateTime("2014-09-01T12:34:56+0100"));
    IssueDoc issue2 = newDoc("I2", file).setFuncCreationDate(parseDateTime("2014-09-01T10:46:00-1200"));
    IssueDoc issue3 = newDoc("I3", file).setFuncCreationDate(parseDateTime("2014-09-02T23:34:56+1200"));
    IssueDoc issue4 = newDoc("I4", file).setFuncCreationDate(parseDateTime("2014-09-05T12:34:56+0100"));
    IssueDoc issue5 = newDoc("I5", file).setFuncCreationDate(parseDateTime("2014-09-20T12:34:56+0100"));
    IssueDoc issue6 = newDoc("I6", file).setFuncCreationDate(parseDateTime("2015-01-18T12:34:56+0100"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  /**
   * Execute the search request and return the document ids of results.
   */
  private List<String> searchAndReturnKeys(IssueQuery.Builder query) {
    return Arrays.stream(underTest.search(query.build(), new SearchOptions()).getHits().getHits())
      .map(SearchHit::getId)
      .collect(Collectors.toList());
  }

  private void assertThatSearchReturnsOnly(IssueQuery.Builder query, String... expectedIssueKeys) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).containsExactlyInAnyOrder(expectedIssueKeys);
  }

  private void assertThatSearchReturnsEmpty(IssueQuery.Builder query) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).isEmpty();
  }
}
