/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualitygate.changeevent;

import java.util.Arrays;
import java.util.Collection;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;

/**
 * Broadcast a given collection of {@link QGChangeEvent} for a specific trigger to all the registered
 * {@link QGChangeEventListener} in Pico.
 *
 * This class ensures that an {@link Exception} occurring calling one of the {@link QGChangeEventListener} doesn't
 * prevent from calling the others.
 */
public class QGChangeEventListenersImpl implements QGChangeEventListeners {
  private static final Logger LOG = Loggers.get(QGChangeEventListenersImpl.class);

  private final QGChangeEventListener[] listeners;

  /**
   * Used by Pico when there is no QGChangeEventListener instance in container.
   */
  public QGChangeEventListenersImpl() {
    this.listeners = new QGChangeEventListener[0];
  }

  public QGChangeEventListenersImpl(QGChangeEventListener[] listeners) {
    this.listeners = listeners;
  }

  @Override
  public boolean isEmpty() {
    return listeners.length == 0;
  }

  @Override
  public void broadcast(Trigger trigger, Collection<QGChangeEvent> changeEvents) {
    try {
      Arrays.stream(listeners).forEach(listener -> broadcastTo(trigger, changeEvents, listener));
    } catch (Error e) {
      LOG.warn(format("Broadcasting to listeners failed for %s events", changeEvents.size()), e);
    }
  }

  private void broadcastTo(Trigger trigger, Collection<QGChangeEvent> changeEvents, QGChangeEventListener listener) {
    try {
      LOG.debug("calling onChange() on listener {} for events {}...", listener.getClass().getName(), changeEvents);
      listener.onChanges(trigger, changeEvents);
    } catch (Exception e) {
      LOG.warn(format("onChange() call failed on listener %s for events %s", listener.getClass().getName(), changeEvents), e);
    }
  }
}
