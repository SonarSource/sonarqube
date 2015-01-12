package org.sonar.batch.phases;

import org.junit.Test;
import org.sonar.batch.index.MeasurePersister;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.batch.index.ScanPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultPhaseExecutorTest {

  @Test
  public void shouldSortPersisters() {
    ScanPersister otherPersister = mock(ScanPersister.class);
    MeasurePersister measurePersister = new MeasurePersister(null, null, null, null, null);
    ResourcePersister resourcePersister = new ResourcePersister(null, null, null, null, null);
    ScanPersister[] persisters = new ScanPersister[] {otherPersister, measurePersister, resourcePersister};
    DefaultPhaseExecutor executor = new DefaultPhaseExecutor(null, null, null, null, null, null,
      null, null, null, null, null, persisters, null, null, null, null, null, null, null);
    assertThat(executor.sortedPersisters()).containsSubsequence(resourcePersister, measurePersister);

    persisters = new ScanPersister[] {measurePersister, resourcePersister, otherPersister};
    executor = new DefaultPhaseExecutor(null, null, null, null, null, null,
      null, null, null, null, null, persisters, null, null, null, null, null, null, null);
    assertThat(executor.sortedPersisters()).containsSubsequence(resourcePersister, measurePersister);

    persisters = new ScanPersister[] {measurePersister, otherPersister, resourcePersister};
    executor = new DefaultPhaseExecutor(null, null, null, null, null, null,
      null, null, null, null, null, persisters, null, null, null, null, null, null, null);
    assertThat(executor.sortedPersisters()).containsSubsequence(resourcePersister, measurePersister);
  }

}
