package org.sonar.scanner.phases;

import org.sonar.api.batch.events.EventHandler;
import org.sonar.api.batch.fs.internal.DefaultInputModule;

@FunctionalInterface
public interface ProjectAnalysisHandler extends EventHandler {

  /**
   * This interface is not intended to be implemented by clients.
   */
  interface ProjectAnalysisEvent {

    DefaultInputModule getProject();

    boolean isStart();

    boolean isEnd();

  }

  /**
   * Called before and after analysis of project.
   */
  void onProjectAnalysis(ProjectAnalysisEvent event);

}
