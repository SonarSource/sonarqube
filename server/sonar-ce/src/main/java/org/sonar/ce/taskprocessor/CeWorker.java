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
package org.sonar.ce.taskprocessor;

import java.util.concurrent.Callable;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTask;

/**
 * Marker interface of the runnable in charge of polling the {@link CeQueue} and executing {@link CeTask}.
 * {@link Callable#call()} returns a Boolean which is {@code true} when some a {@link CeTask} was processed,
 * {@code false} otherwise.
 */
public interface CeWorker extends Callable<Boolean> {
  String getUUID();
}
