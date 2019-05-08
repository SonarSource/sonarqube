/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.process.cluster.hz;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.core.MultiExecutionCallback;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

class HazelcastMemberImpl implements HazelcastMember {

  private final HazelcastInstance hzInstance;

  HazelcastMemberImpl(HazelcastInstance hzInstance) {
    this.hzInstance = hzInstance;
  }

  @Override
  public <E> IAtomicReference<E> getAtomicReference(String name) {
    return hzInstance.getAtomicReference(name);
  }

  @Override
  public <K, V> Map<K, V> getReplicatedMap(String s) {
    return hzInstance.getReplicatedMap(s);
  }

  @Override
  public String getUuid() {
    return hzInstance.getLocalEndpoint().getUuid();
  }

  @Override
  public Set<String> getMemberUuids() {
    return hzInstance.getCluster().getMembers().stream().map(Member::getUuid).collect(Collectors.toSet());
  }

  @Override
  public Lock getLock(String s) {
    return hzInstance.getLock(s);
  }

  @Override
  public long getClusterTime() {
    return hzInstance.getCluster().getClusterTime();
  }

  @Override
  public Cluster getCluster() {
    return hzInstance.getCluster();
  }

  @Override
  public <T> DistributedAnswer<T> call(DistributedCall<T> callable, MemberSelector memberSelector, long timeoutMs)
    throws InterruptedException {

    IExecutorService executor = hzInstance.getExecutorService("default");
    Map<Member, Future<T>> futures = executor.submitToMembers(callable, memberSelector);
    try {
      DistributedAnswer<T> distributedAnswer = new DistributedAnswer<>();
      long maxTime = System.currentTimeMillis() + timeoutMs;
      for (Map.Entry<Member, Future<T>> entry : futures.entrySet()) {
        long remainingMs = Math.max(maxTime - System.currentTimeMillis(), 5L);
        try {
          T answer = entry.getValue().get(remainingMs, TimeUnit.MILLISECONDS);
          distributedAnswer.setAnswer(entry.getKey(), answer);
        } catch (ExecutionException e) {
          distributedAnswer.setFailed(entry.getKey(), e);
        } catch (TimeoutException e) {
          distributedAnswer.setTimedOut(entry.getKey());
        }
      }
      return distributedAnswer;
    } finally {
      futures.values().forEach(f -> f.cancel(true));
    }
  }

  @Override
  public <T> void callAsync(DistributedCall<T> callable, MemberSelector memberSelector, DistributedCallback<T> callback) {
    IExecutorService executor = hzInstance.getExecutorService("default");

    // callback doesn't handle failures, so we need to make sure the callable won't fail!
    executor.submitToMembers(callable, memberSelector, new MultiExecutionCallback() {
      @Override
      public void onResponse(Member member, Object value) {
        // nothing to do when each node responds
      }

      @Override
      public void onComplete(Map<Member, Object> values) {
        callback.onComplete((Map<Member, T>) values);
      }
    });
  }

  @Override
  public void close() {
    try {
      hzInstance.shutdown();
    } catch (HazelcastInstanceNotActiveException e) {
      LoggerFactory.getLogger(getClass()).debug("Unable to shutdown Hazelcast member", e);
    }
  }
}
