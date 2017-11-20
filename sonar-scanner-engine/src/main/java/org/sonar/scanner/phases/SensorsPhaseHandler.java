package org.sonar.scanner.phases;

import java.util.List;
import org.sonar.api.batch.events.EventHandler;
import org.sonar.api.batch.sensor.Sensor;

@FunctionalInterface
public interface SensorsPhaseHandler extends EventHandler {

  /**
   * This interface is not intended to be implemented by clients.
   */
  interface SensorsPhaseEvent {

    /**
     * @return list of Sensors in the order of execution
     */
    List<Sensor> getSensors();

    boolean isStart();

    boolean isEnd();

  }

  /**
   * Called before and after execution of all {@link Sensor}s.
   */
  void onSensorsPhase(SensorsPhaseEvent event);

}
