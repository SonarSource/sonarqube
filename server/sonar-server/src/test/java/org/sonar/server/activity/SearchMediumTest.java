package org.sonar.server.activity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;

import java.lang.management.ManagementFactory;

public abstract class SearchMediumTest {


  private static Logger LOGGER = LoggerFactory.getLogger(SearchMediumTest.class);

  @ClassRule
  public static ServerTester tester = new ServerTester();

  protected DbClient db;
  protected DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);

    ManagementFactory.getMemoryMXBean();
    LOGGER.info("* Environment ({})", ManagementFactory.getOperatingSystemMXBean().getName());
    LOGGER.info("* heap:\t{}", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
    LOGGER.info("* load:\t{}", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
  }

  @After
  public void after() {
    dbSession.close();
  }

}
