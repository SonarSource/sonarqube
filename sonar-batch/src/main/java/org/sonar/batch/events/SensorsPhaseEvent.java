package org.sonar.batch.events;

import org.sonar.api.batch.Sensor;

import java.util.Collection;

/**
 * Fired before execution of {@link Sensor}s and after.
 */
public class SensorsPhaseEvent extends SonarEvent<SensorsPhaseHandler> {

  private Collection<Sensor> sensors;
  private boolean start;

  public SensorsPhaseEvent(Collection<Sensor> sensors, boolean start) {
    this.sensors = sensors;
    this.start = start;
  }

  public Collection<Sensor> getSensors() {
    return sensors;
  }

  public boolean isPhaseStart() {
    return start;
  }

  @Override
  protected void dispatch(SensorsPhaseHandler handler) {
    handler.onSensorsPhase(this);
  }

  @Override
  protected Class getType() {
    return SensorsPhaseHandler.class;
  }

}
