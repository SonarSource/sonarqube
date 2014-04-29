/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.cluster.IndexAction;
import org.sonar.core.cluster.WorkQueue;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalNonBlockingWorkQueue implements WorkQueue {

  private static final Logger LOG = LoggerFactory.getLogger(LocalNonBlockingWorkQueue.class);

  private final ConcurrentLinkedQueue<IndexAction<?>> actions;

  public LocalNonBlockingWorkQueue() {
    this.actions = new ConcurrentLinkedQueue<IndexAction<?>>();
  }

  @Override
  public Integer enqueue(IndexAction<?>... indexActions) {
    for (IndexAction<?> action : indexActions) {
      actions.offer(action);
    }
    return 0;
  }

  @Override
  public IndexAction<?> dequeue() {
    IndexAction<?> out = actions.poll();
    return out;
  }
}
