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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.server.search.action.EmbeddedIndexAction;
import org.sonar.server.search.action.IndexAction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IndexQueue extends LinkedBlockingQueue<Runnable>
  implements ServerComponent, WorkQueue<IndexAction> {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexQueue.class);

  private static final Integer DEFAULT_QUEUE_SIZE = 200;
  private static final int TIMEOUT = 3000;

  public IndexQueue() {
    super(DEFAULT_QUEUE_SIZE);
  }

  @Override
  public void enqueue(IndexAction action) {
    this.enqueue(ImmutableList.of(action));
  }

  @Override
  public void enqueue(List<IndexAction> actions) {

    int bcount = 0;
    int ecount = 0;
    List<String> refreshes = Lists.newArrayList();
    Set<String> types = Sets.newHashSet();
    long all_start = System.currentTimeMillis();
    long indexTime;
    long refreshTime;
    long embeddedTime;

    if (actions.size() == 1) {
      /* Atomic update here */
      CountDownLatch latch = new CountDownLatch(1);
      IndexAction action = actions.get(0);
      action.setLatch(latch);
      try {
        indexTime = System.currentTimeMillis();
        this.offer(action, TIMEOUT, TimeUnit.SECONDS);
        if (!latch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
          throw new IllegalStateException("ES update could not be completed within: " + TIMEOUT + "ms");
        }
        bcount++;
        indexTime = System.currentTimeMillis() - indexTime;
        // refresh the index.
        Index<?, ?, ?> index = action.getIndex();
        if (index != null) {
          refreshTime = System.currentTimeMillis();
          index.refresh();
          refreshTime = System.currentTimeMillis() - refreshTime;
          refreshes.add(index.getIndexName());
        }
        types.add(action.getPayloadClass().getSimpleName());
      } catch (InterruptedException e) {
        throw new IllegalStateException("ES update has been interrupted", e);
      }
    } else if (actions.size() > 1) {

      /* Purge actions that would be overridden  */
      Long purgeStart = System.currentTimeMillis();
      List<IndexAction> itemActions = Lists.newArrayList();
      List<IndexAction> embeddedActions = Lists.newArrayList();

      for (IndexAction action : actions) {
        if (action.getClass().isAssignableFrom(EmbeddedIndexAction.class)) {
          embeddedActions.add(action);
        } else {
          itemActions.add(action);
        }
      }

      LOGGER.debug("INDEX - compressed {} items into {} in {}ms,",
        actions.size(), itemActions.size() + embeddedActions.size(), System.currentTimeMillis() - purgeStart);

      try {
        /* execute all item actions */
        CountDownLatch itemLatch = new CountDownLatch(itemActions.size());
        indexTime = System.currentTimeMillis();
        for (IndexAction action : itemActions) {
          action.setLatch(itemLatch);
          this.offer(action, TIMEOUT, TimeUnit.SECONDS);
          types.add(action.getPayloadClass().getSimpleName());
          bcount++;

        }
        if (!itemLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
          throw new IllegalStateException("ES update could not be completed within: " + TIMEOUT + "ms");
        }
        indexTime = System.currentTimeMillis() - indexTime;

        /* and now push the embedded */
        CountDownLatch embeddedLatch = new CountDownLatch(embeddedActions.size());
        embeddedTime = System.currentTimeMillis();
        for (IndexAction action : embeddedActions) {
          action.setLatch(embeddedLatch);
          this.offer(action, TIMEOUT, TimeUnit.SECONDS);
          types.add(action.getPayloadClass().getSimpleName());
          ecount++;
        }
        if (!embeddedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
          throw new IllegalStateException("ES embedded update could not be completed within: " + TIMEOUT + "ms");
        }
        embeddedTime = System.currentTimeMillis() - embeddedTime;

        /* Finally refresh affected indexes */
        Set<String> refreshedIndexes = new HashSet<String>();
        refreshTime = System.currentTimeMillis();
        for (IndexAction action : actions) {
          if (action.getIndex() != null &&
            !refreshedIndexes.contains(action.getIndex().getIndexName())) {
            refreshedIndexes.add(action.getIndex().getIndexName());
            action.getIndex().refresh();
            refreshes.add(action.getIndex().getIndexName());
          }
        }
        refreshTime = System.currentTimeMillis() - refreshTime;
      } catch (InterruptedException e) {
        throw new IllegalStateException("ES update has been interrupted", e);
      }
      LOGGER.debug("INDEX - time:{}ms ({}ms index, {}ms embedded, {}ms refresh)\ttypes:[{}],\tbulk:{}\tembedded:{}\trefresh:[{}]",
        (System.currentTimeMillis() - all_start), indexTime, embeddedTime, refreshTime,
        StringUtils.join(types, ","),
        bcount, ecount, StringUtils.join(refreshes, ","));
    }
  }
}
