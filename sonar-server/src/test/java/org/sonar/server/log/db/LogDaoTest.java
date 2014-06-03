package org.sonar.server.log.db;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class LogDaoTest extends AbstractDaoTestCase{


  private LogDao dao;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() throws Exception {
    this.session = getMyBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new LogDao(system2);
  }

  @After
  public void after(){
    session.close();
  }

  @Test
  public void insert_log(){

    TestActivity activity = new TestActivity("hello world");

    System.out.println("activity.getClass().getName() = " + activity.getClass().getName());
    
    LogDto log = new LogDto("SYSTEM_USER", activity);

    dao.insert(session, log);

    LogDto newDto = dao.getByKey(session, log.getKey());
    assertThat(newDto.getAuthor()).isEqualTo(log.getAuthor());

    TestActivity newActivity = newDto.getActivity();
    assertThat(newActivity.test).isEqualTo("hello world");

  }
}