package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.picocontainer.Startable;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BootstrapExtensionExecutorTest {
  private ProjectReactor reactor = new ProjectReactor(ProjectDefinition.create().setKey("foo"));

  @Test
  public void start() {
    ComponentContainer container = new ComponentContainer();
    // dependencies required for ProjectExclusions
    container.addSingleton(reactor);
    container.addSingleton(new Settings());

    // declare a bootstrap component
    final Startable bootstrapComponent = mock(Startable.class);
    ExtensionInstaller installer = mock(ExtensionInstaller.class);
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        ComponentContainer childContainer = (ComponentContainer) invocationOnMock.getArguments()[0];
        childContainer.addSingleton(bootstrapComponent);
        return null;
      }
    }).when(installer).install(any(ComponentContainer.class), eq(InstantiationStrategy.BOOTSTRAP));

    BootstrapExtensionExecutor executor = new BootstrapExtensionExecutor(container, installer);
    executor.start();

    // should install bootstrap components into a ephemeral container
    verify(installer).install(any(ComponentContainer.class), eq(InstantiationStrategy.BOOTSTRAP));
    verify(bootstrapComponent).start();
    verify(bootstrapComponent).stop();

    // the ephemeral container is destroyed
    assertThat(container.getComponentByType(ProjectExclusions.class)).isNull();
    assertThat(container.getChild()).isNull();
  }


}
