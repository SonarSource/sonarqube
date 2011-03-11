package org.sonar.batch.events;

import org.sonar.api.batch.Decorator;

import java.util.Collection;

/**
 * Fired before execution of {@link Decorator}s and after.
 */
public class DecoratorsPhaseEvent extends SonarEvent<DecoratorsPhaseHandler> {

  private Collection<Decorator> decorators;
  private boolean start;

  public DecoratorsPhaseEvent(Collection<Decorator> decorators, boolean start) {
    this.decorators = decorators;
    this.start = start;
  }

  public Collection<Decorator> getDecorators() {
    return decorators;
  }

  public boolean isPhaseStart() {
    return start;
  }

  public boolean isPhaseDone() {
    return !start;
  }

  @Override
  protected void dispatch(DecoratorsPhaseHandler handler) {
    handler.onDecoratorsPhase(this);
  }

  @Override
  protected Class getType() {
    return DecoratorsPhaseHandler.class;
  }

}
