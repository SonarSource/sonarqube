package org.sonar.server.qualityprofile;

import org.elasticsearch.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.log.Log;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.log.LogService;
import org.sonar.server.log.index.LogIndex;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleChangeMediumTest {


  @ClassRule
  public static ServerTester tester = new ServerTester();

  LogService service = tester.get(LogService.class);
  LogIndex index = tester.get(LogIndex.class);
  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_find_active_rule_change() {
    ActiveRuleChange change = new ActiveRuleChange()
      .setInheritance(ActiveRule.Inheritance.INHERITED)
      .setSeverity("BLOCKER")
      .setParameter("param1", "value1");

    service.write(dbSession, Log.Type.ACTIVE_RULE, change);
    dbSession.commit();

    // 0. AssertBase case
    assertThat(index.findAll().getHits()).hasSize(1);

    Log log = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(log).isNotNull();
  }
}