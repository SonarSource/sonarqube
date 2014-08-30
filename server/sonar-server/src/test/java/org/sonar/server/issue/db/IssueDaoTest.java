package org.sonar.server.issue.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssueDaoTest extends AbstractDaoTestCase {

  private IssueDao dao;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() throws Exception {
    this.session = getMyBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new IssueDao(system2);
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void find_after_dates() throws Exception {
    setupData("shared", "should_select_all");

    Date t0 = new Date(0);
    assertThat(dao.findAfterDate(session, t0)).hasSize(3);

    Date t2014 = DateUtils.parseDate("2014-01-01");
    assertThat(dao.findAfterDate(session, t2014)).hasSize(1);
  }
}
