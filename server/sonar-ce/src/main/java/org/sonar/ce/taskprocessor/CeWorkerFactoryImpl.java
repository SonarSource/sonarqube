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
package org.sonar.ce.taskprocessor;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.ce.queue.InternalCeQueue;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;

public class CeWorkerFactoryImpl implements CeWorkerFactory {
  private final UuidFactory uuidFactory;
  private final InternalCeQueue queue;
  private final CeTaskProcessorRepository taskProcessorRepository;
  private final CeWorkerController ceWorkerController;
  private final CeWorker.ExecutionListener[] executionListeners;
  private Set<CeWorker> ceWorkers = Collections.emptySet();

  /**
   * Used by Pico when there is no {@link CeWorker.ExecutionListener} in the container.
   */
  public CeWorkerFactoryImpl(InternalCeQueue queue, CeTaskProcessorRepository taskProcessorRepository,
    UuidFactory uuidFactory, CeWorkerController ceWorkerController) {
    this(queue, taskProcessorRepository, uuidFactory, ceWorkerController, new CeWorker.ExecutionListener[0]);
  }

  public CeWorkerFactoryImpl(InternalCeQueue queue, CeTaskProcessorRepository taskProcessorRepository,
    UuidFactory uuidFactory, CeWorkerController ceWorkerController,
    CeWorker.ExecutionListener[] executionListeners) {
    this.queue = queue;
    this.taskProcessorRepository = taskProcessorRepository;
    this.uuidFactory = uuidFactory;
    this.ceWorkerController = ceWorkerController;
    this.executionListeners = executionListeners;
  }

  @Override
  public CeWorker create(int ordinal) {
    String uuid = uuidFactory.create();
    CeWorkerImpl ceWorker = new CeWorkerImpl(ordinal, uuid, queue, taskProcessorRepository, ceWorkerController, executionListeners);
    ceWorkers = Stream.concat(ceWorkers.stream(), Stream.of(ceWorker)).collect(MoreCollectors.toSet(ceWorkers.size() + 1));
    return ceWorker;
  }

  @Override
  public Set<CeWorker> getWorkers() {
    return ceWorkers;
  }
}
