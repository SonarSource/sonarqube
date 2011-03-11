package org.sonar.batch.events;

public interface SensorsPhaseHandler extends EventHandler {

  void onSensorsPhase(SensorsPhaseEvent event);

}
