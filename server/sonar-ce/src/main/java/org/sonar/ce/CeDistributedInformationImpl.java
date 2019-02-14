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
package org.sonar.ce;

import com.hazelcast.spi.exception.RetryableHazelcastException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.taskprocessor.CeWorker;
import org.sonar.ce.taskprocessor.CeWorkerFactory;
import org.sonar.process.cluster.hz.HazelcastMember;
import org.sonar.process.cluster.hz.HazelcastObjects;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.process.cluster.hz.HazelcastObjects.WORKER_UUIDS;

/**
 * Provide the set of worker's UUID in a clustered SonarQube instance
 */
public class CeDistributedInformationImpl implements CeDistributedInformation, Startable {
  private static final Logger LOGGER = Loggers.get(CeDistributedInformationImpl.class);

  private final HazelcastMember hazelcastMember;
  private final CeWorkerFactory ceCeWorkerFactory;

  public CeDistributedInformationImpl(HazelcastMember hazelcastMember, CeWorkerFactory ceCeWorkerFactory) {
    this.hazelcastMember = hazelcastMember;
    this.ceCeWorkerFactory = ceCeWorkerFactory;
  }

  @Override
  public Set<String> getWorkerUUIDs() {
    Set<String> connectedWorkerUUIDs = hazelcastMember.getMemberUuids();

    return getClusteredWorkerUUIDs().entrySet().stream()
      .filter(e -> connectedWorkerUUIDs.contains(e.getKey()))
      .map(Map.Entry::getValue)
      .flatMap(Set::stream)
      .collect(toSet());
  }

  @Override
  public void broadcastWorkerUUIDs() {
    Set<CeWorker> workers = ceCeWorkerFactory.getWorkers();
    Set<String> workerUuids = workers.stream().map(CeWorker::getUUID).collect(toSet(workers.size()));
    getClusteredWorkerUUIDs().put(hazelcastMember.getUuid(), workerUuids);
  }

  @Override
  public Lock acquireCleanJobLock() {
    return hazelcastMember.getLock(HazelcastObjects.CE_CLEANING_JOB_LOCK);
  }

  @Override
  public void start() {
    // Nothing to do here
  }

  @Override
  public void stop() {
    try {
      // Removing the worker UUIDs
      getClusteredWorkerUUIDs().remove(hazelcastMember.getUuid());
    } catch (RetryableHazelcastException e) {
      LOGGER.debug("Unable to remove worker UUID from the list of active workers", e.getMessage());
    }
  }

  private Map<String, Set<String>> getClusteredWorkerUUIDs() {
    return hazelcastMember.getReplicatedMap(WORKER_UUIDS);
  }
}
