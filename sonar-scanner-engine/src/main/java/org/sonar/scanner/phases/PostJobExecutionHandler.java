package org.sonar.scanner.phases;

import org.sonar.api.batch.events.EventHandler;
import org.sonar.api.batch.postjob.PostJob;

@FunctionalInterface
public interface PostJobExecutionHandler extends EventHandler {

  /**
   * This interface is not intended to be implemented by clients.
   */
  interface PostJobExecutionEvent {

    PostJob getPostJob();

    boolean isStart();

    boolean isEnd();

  }

  /**
   * Called before and after execution of {@link PostJob}.
   */
  void onPostJobExecution(PostJobExecutionEvent event);

}
