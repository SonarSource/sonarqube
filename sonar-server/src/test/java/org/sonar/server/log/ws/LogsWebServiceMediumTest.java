package org.sonar.server.log.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.log.Log;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.log.LogService;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class LogsWebServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private LogsWebService ws;
  private LogService service;
  private DbSession session;


  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    ws = tester.get(LogsWebService.class);
    service = tester.get(LogService.class);
    session = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void define() throws Exception {
    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller(LogsWebService.API_ENDPOINT);

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(1);
    assertThat(controller.action(SearchAction.SEARCH_ACTION)).isNotNull();
  }

  @Test
  public void search_logs() throws Exception {
    service.write(session, Log.Type.ACTIVE_RULE, "Hello World");
    session.commit();

    MockUserSession.set();

    // 1. List single Text log
    WsTester.TestRequest request = tester.wsTester().newGetRequest(LogsWebService.API_ENDPOINT, SearchAction.SEARCH_ACTION);
    WsTester.Result result = request.execute();
    System.out.println("result = " + result.outputAsString());
  }


}
