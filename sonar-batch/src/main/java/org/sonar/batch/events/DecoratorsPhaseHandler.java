package org.sonar.batch.events;

public interface DecoratorsPhaseHandler extends EventHandler {

  void onDecoratorsPhase(DecoratorsPhaseEvent event);

}
