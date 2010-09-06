package org.sonar.batch;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import org.junit.Test;

import java.util.List;

public class CoreJobsTest {

  @Test
  public void mavenPluginsAreExecutedAfterBeingConfigured() {
    List<Class<? extends CoreJob>> jobs = CoreJobs.allJobs();
    assertThat(jobs.indexOf(FinalizeSnapshotsJob.class),
        greaterThan(jobs.indexOf(DecoratorsExecutor.class)));
  }

  @Test
  public void finalizeJobIsExecutedAfterDecorators() {
    List<Class<? extends CoreJob>> jobs = CoreJobs.allJobs();
    assertThat(jobs.indexOf(FinalizeSnapshotsJob.class),
        greaterThan(jobs.indexOf(DecoratorsExecutor.class)));
  }

  @Test
  public void allJobs() {
    assertThat(CoreJobs.allJobs().size(), greaterThan(3));
  }
}
