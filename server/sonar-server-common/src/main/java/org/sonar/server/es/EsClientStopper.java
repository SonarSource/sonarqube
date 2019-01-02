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
package org.sonar.server.es;

import org.sonar.api.Startable;

/**
 * Workaround of a behaviour of picocontainer: components
 * instantiated by {@link org.picocontainer.injectors.ProviderAdapter}
 * can't have a lifecycle. The methods start() and stop()
 * of {@link Startable} are not executed.
 * The same behaviour exists for the {@link org.picocontainer.injectors.ProviderAdapter}
 * itself.
 *
 * As {@link EsClientStopper} implements {@link Startable}, it can
 * close {@link EsClient} when process shutdowns.
 *
 */
public class EsClientStopper implements Startable {

  private final EsClient esClient;

  public EsClientStopper(EsClient esClient) {
    this.esClient = esClient;
  }

  @Override
  public void start() {
    // nothing to do
  }

  @Override
  public void stop() {
    esClient.close();
  }
}
