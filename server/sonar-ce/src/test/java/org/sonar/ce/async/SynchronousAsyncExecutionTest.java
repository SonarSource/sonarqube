package org.sonar.ce.async;

import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SynchronousAsyncExecutionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SynchronousAsyncExecution underTest = new SynchronousAsyncExecution();

  @Test
  public void addToQueue_fails_with_NPE_if_Runnable_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.addToQueue(null);
  }

  @Test
  public void addToQueue_executes_Runnable_synchronously() {
    Set<String> s = new HashSet<>();

    underTest.addToQueue(() -> s.add("done"));

    assertThat(s).containsOnly("done");
  }
}
