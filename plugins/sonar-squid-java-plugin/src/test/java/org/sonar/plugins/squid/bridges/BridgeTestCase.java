package org.sonar.plugins.squid.bridges;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.checkers.MessageDispatcher;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.squid.SquidExecutor;
import org.sonar.plugins.squid.SquidTestUtils;

public abstract class BridgeTestCase {

  protected SensorContext context;
  private static SquidExecutor executor;
  protected static Project project;

  @BeforeClass
  public static void scanStruts() throws IOException, URISyntaxException {
    executor = new SquidExecutor(true, "LOG, logger", new MessageDispatcher(mock(SensorContext.class)), Charset.forName("UTF8"));
    executor.scan(SquidTestUtils.getStrutsCoreSources(), Arrays.asList(SquidTestUtils.getStrutsCoreJar()));
    project = new Project("project");
  }

  @Before
  public final void saveData() {
    context = mock(SensorContext.class);
    when(context.getResource((Resource) anyObject())).thenAnswer(new Answer<Resource>() {

      public Resource answer(InvocationOnMock invocationOnMock) throws Throwable {
        return (Resource) invocationOnMock.getArguments()[0];
      }
    });
    executor.save(project, context, null);
  }
}
