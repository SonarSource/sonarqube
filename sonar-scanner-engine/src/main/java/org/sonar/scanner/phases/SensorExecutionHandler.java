package org.sonar.scanner.phases;

import org.sonar.api.batch.events.EventHandler;
import org.sonar.api.batch.sensor.Sensor;

@FunctionalInterface
public interface SensorExecutionHandler extends EventHandler {

  /**
   * This interface is not intended to be implemented by clients.
   */
  interface SensorExecutionEvent {

    Sensor getSensor();

    boolean isStart();

    boolean isEnd();

  }

  /**
   * Called before and after execution of {@link Sensor}.
   */
  void onSensorExecution(SensorExecutionEvent event);

}
