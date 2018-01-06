/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api;

/**
 * An interface which is implemented by classes annotated with {@link org.sonar.api.batch.ScannerSide ScannerSide},
 * {@link org.sonar.api.server.ServerSide ServerSide} and/or {@link org.sonar.api.ce.ComputeEngineSide ComputeEngineSide}
 * (referred to below as "component") that can be started and stopped.
 * <p>
 * The method {@link #start()} is called at the begin of the component lifecycle.
 * It can be called again only after a call to {@link #stop()}. The {@link #stop()} method is called at the end of the
 * component lifecycle, and can further be called after every {@link Startable#start()}.
 * </p>
 * <p>
 * In the WebServer, a component is started once: either right when the WebServer is started if there is no migration,
 * otherwise only after Database has been successfully migrated. It is stopped once when the WebServer is shutdown. Any
 * exception thrown by method {@link #start()} will make the WebServer startup fail.
 * </p>
 * <p>
 * In the Compute Engine, a component is started once when the Compute Engine is started and stopped once when the
 * ComputeEngine is shut down. Any exception thrown by method {@link #start()} will make the Compute Engine startup fail.
 * </p>
 * <p>
 * On Scanner side, the lifecycle of a component depends on the value of the {@link org.sonar.api.batch.InstantiationStrategy
 * InstantiationStrategy} annotation.
 * </p>
 */
public interface Startable {
  /**
   * Start this component. Called initially at the begin of the lifecycle. It can be called again after a stop.
   */
  void start();

  /**
   * Stop this component. Called near the end of the lifecycle. It can be called again after a further start.
   */
  void stop();
}
