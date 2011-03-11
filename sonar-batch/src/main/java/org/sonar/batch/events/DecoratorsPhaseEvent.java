/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
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
