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
package org.sonar.server.search;

import com.google.common.collect.ImmutableList;
import org.sonar.api.ServerComponent;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.server.search.action.IndexAction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IndexQueue extends LinkedBlockingQueue<Runnable>
  implements ServerComponent, WorkQueue<IndexAction> {

  private static final Integer DEFAULT_QUEUE_SIZE = 20;

  public IndexQueue() {
    super(DEFAULT_QUEUE_SIZE);
  }

  @Override
  public void enqueue(IndexAction action) {
    this.enqueue(ImmutableList.of(action));
  }

  @Override
  public void enqueue(List<IndexAction> actions) {

    if (actions.size() == 1) {
      /* Atomic update here */
      CountDownLatch latch = new CountDownLatch(1);
      IndexAction action = actions.get(0);
      action.setLatch(latch);
      try {
        this.offer(action, 1000, TimeUnit.SECONDS);
        latch.await(1500, TimeUnit.MILLISECONDS);
        // refresh the index.
        action.getIndex().refresh();
      } catch (InterruptedException e) {
        throw new IllegalStateException("ES update has been interrupted", e);
      }
    } else if (actions.size() > 1) {
      /* Bulkize set of update */

      // DTO action -> take the latest in any Method

      // Key action -> clears the stack for DTO based on key for any method

      // Object actions -> after any other stack for its key from DTO and Key stack

      CountDownLatch latch = new CountDownLatch(actions.size());
      try {
        for (IndexAction action : actions) {
          action.setLatch(latch);
          this.offer(action, 1000, TimeUnit.SECONDS);
        }
        latch.await(1500, TimeUnit.MILLISECONDS);

        //Now all actions must have their index != null;
        Set<String> refreshedIndexes = new HashSet<String>();
        for (IndexAction action : actions) {
          if (!refreshedIndexes.contains(action.getIndexType())) {
            action.getIndex().refresh();
            refreshedIndexes.add(action.getIndexType());
          }
        }
      } catch (InterruptedException e) {
        throw new IllegalStateException("ES update has been interrupted", e);
      }
    }
  }
}
