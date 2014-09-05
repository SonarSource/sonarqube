package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.tester.ServerTester;

import java.util.UUID;

import static org.fest.assertions.Assertions.assertThat;

public class IssueIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  IssueIndex index;

  RuleDto rule;
  ComponentDto project;
  ComponentDto resource;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    index = tester.get(IssueIndex.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(session, project);

    resource = new ComponentDto()
      .setProjectId(1L)
      .setKey("MyComponent")
      .setId(2L);
    tester.get(ComponentDao.class).insert(session, resource);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void filter_by_actionPlan() throws Exception {

    String plan1 = "plan1";
    String plan2 = "plan2";
    IssueDto issue1 = getIssue()
      .setActionPlanKey(plan1);
    IssueDto issue2 = getIssue()
      .setActionPlanKey(plan2);
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    query.actionPlans(ImmutableList.of(plan1));
    SearchResponse result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(1L);

    query = IssueQuery.builder();
    query.actionPlans(ImmutableList.of(plan2));
    result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(1L);

    query = IssueQuery.builder();
    query.actionPlans(ImmutableList.of(plan2, plan1));
    result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(2L);
  }

  @Test
  public void is_assigned_filter() throws Exception {

    String assignee = "steph";
    IssueDto issue1 = getIssue()
      .setAssignee(assignee);
    IssueDto issue2 = getIssue();
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    SearchResponse result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(2L);

    query = IssueQuery.builder();
    query.assigned(true);
    result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(1L);
  }

  @Test
  public void filter_assignee() throws Exception {

    String assignee1 = "steph";
    String assignee2 = "simon";
    IssueDto issue1 = getIssue()
      .setAssignee(assignee1);
    IssueDto issue2 = getIssue()
      .setAssignee(assignee2);
    IssueDto issue3 = getIssue();
    db.issueDao().insert(session, issue1, issue2, issue3);
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    SearchResponse result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(3L);

    query = IssueQuery.builder();
    query.assignees(ImmutableList.of(assignee1));
    result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(1L);

    query = IssueQuery.builder();
    query.assignees(ImmutableList.of(assignee1, assignee2));
    result = index.search(query.build(), new QueryOptions());
    assertThat(result.getHits().getTotalHits()).isEqualTo(2L);
  }

  private IssueDto getIssue() {
    return new IssueDto()
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setRootComponent(project)
      .setComponent(resource)
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setKee(UUID.randomUUID().toString());
  }
}
