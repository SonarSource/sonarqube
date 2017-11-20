package org.sonar.scanner.phases;

import java.util.List;
import org.sonar.api.batch.events.EventHandler;
import org.sonar.api.batch.postjob.PostJob;

@FunctionalInterface
public interface PostJobsPhaseHandler extends EventHandler {

  /**
   * This interface is not intended to be implemented by clients.
   */
  interface PostJobsPhaseEvent {

    /**
     * @return list of PostJob in the order of execution
     */
    List<PostJob> getPostJobs();

    boolean isStart();

    boolean isEnd();

  }

  /**
   * Called before and after execution of all {@link PostJob}s.
   */
  void onPostJobsPhase(PostJobsPhaseEvent event);

}
