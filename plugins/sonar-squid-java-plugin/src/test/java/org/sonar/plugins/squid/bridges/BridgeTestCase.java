package org.sonar.plugins.squid.bridges;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.BeforeClass;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.squid.SquidExecutor;
import org.sonar.plugins.squid.SquidTestUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

public abstract class BridgeTestCase {

  protected SensorContext context;
  private static SquidExecutor executor;
  protected static Project project;

  @BeforeClass
  public static void scanStruts() throws IOException, URISyntaxException {
    RulesProfile profile = RulesProfile.create();
    CheckFactory checkFactory = AnnotationCheckFactory.create(profile, "repo", Collections.<Class> emptyList());
    executor = new SquidExecutor(true, "LOG, logger", checkFactory, Charset.forName("UTF8"));
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
