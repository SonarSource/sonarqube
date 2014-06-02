package org.sonar.server.log.db;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.log.LogDto;
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
    LogDto log = LogDto.newSystemLog();
    dao.insert(session, log);

    LogDto newdto = dao.getByKey(session, log.getKey());
    assertThat(newdto.getAuthor()).isEqualTo(log.getAuthor());

  }
}