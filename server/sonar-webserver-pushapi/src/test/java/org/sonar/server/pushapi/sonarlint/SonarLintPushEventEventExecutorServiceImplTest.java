package org.sonar.server.pushapi.sonarlint;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarLintPushEventEventExecutorServiceImplTest {

  @Test
  public void create_executor() {
    assertThat(SonarLintPushEventEventExecutorServiceImpl.createThread(() -> {
    }))
      .extracting(Thread::getPriority, Thread::isDaemon, thread -> thread.getName().startsWith("SonarLint-PushEvent-"))
      .containsExactly(Thread.MIN_PRIORITY, true, true);
  }
}