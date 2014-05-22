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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.sonar.api.ServerComponent;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.server.search.action.EmbeddedIndexAction;
import org.sonar.server.search.action.IndexAction;
import org.sonar.server.search.action.KeyIndexAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

      /* Purge actions that would be overridden  */
      Map<String, Integer> itemOffset = new HashMap<String, Integer>();
      ArrayListMultimap<String, IndexAction> itemActions = ArrayListMultimap.create();
      List<IndexAction> embeddedActions = new LinkedList<IndexAction>();
      for (IndexAction action : actions) {
        if(EmbeddedIndexAction.class.isAssignableFrom(action.getClass())){
          embeddedActions.add(action);
        } else {
          String actionKey = action.getKey();
          Integer offset = 0;
          if(!itemOffset.containsKey(actionKey)){
            itemOffset.put(actionKey, offset);
            itemActions.put(actionKey, action);
          } else {
            offset = itemOffset.get(actionKey);
            if(KeyIndexAction.class.isAssignableFrom(action.getClass())){
              itemOffset.put(actionKey, 0);
              itemActions.get(actionKey).set(0, action);
            } else {
              itemActions.get(actionKey).set(offset, action);
            }
          }
        }
      }

      try {
        /* execute all item actions */
        Multimap<String, IndexAction> itemBulks = makeBulkByType(itemActions);
          CountDownLatch itemLatch = new CountDownLatch(itemBulks.size());
        for (IndexAction action : itemBulks.values()) {
          action.setLatch(itemLatch);
          this.offer(action, 1000, TimeUnit.SECONDS);
        }
        itemLatch.await(1500, TimeUnit.MILLISECONDS);

        /* and now push the embedded */
        Multimap<String, IndexAction> embeddedBulks = makeBulkByType(itemActions);
        CountDownLatch embeddedLatch = new CountDownLatch(embeddedBulks.size());
        for (IndexAction action : embeddedBulks.values()) {
          action.setLatch(embeddedLatch);
          this.offer(action, 1000, TimeUnit.SECONDS);
        }
        embeddedLatch.await(1500, TimeUnit.MILLISECONDS);

        /* Finally refresh affected indexes */
        Set<String> refreshedIndexes = new HashSet<String>();
        for (IndexAction action : actions) {
          if (action.getIndex() != null &&
            !refreshedIndexes.contains(action.getIndex().getIndexName())){
            action.getIndex().refresh();
            refreshedIndexes.add(action.getIndex().getIndexName());
          }
        }

      } catch (InterruptedException e) {
        throw new IllegalStateException("ES update has been interrupted", e);
      }
    }
  }

  private Multimap<String, IndexAction> makeBulkByType(ArrayListMultimap<String, IndexAction> itemActions) {
    Multimap<String, IndexAction> bulks = LinkedListMultimap.create();
    for (IndexAction action : itemActions.values()) {
      bulks.put(action.getIndexType(), action);
    }
    return bulks;
  }

}
