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
package ce;

import java.util.List;
import org.picocontainer.Startable;
import org.sonar.plugin.ce.ReportAnalysisComponentProvider;
import org.sonar.server.computation.task.container.EagerStart;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class ComponentBombReportAnalysisComponentProvider implements ReportAnalysisComponentProvider {
  private final BombConfig bombConfig;

  public ComponentBombReportAnalysisComponentProvider(BombConfig bombConfig) {
    this.bombConfig = bombConfig;
  }

  @Override
  public List<Object> getComponents() {
    if (bombConfig.isOomStartBomb()) {
      return singletonList(OOMFailingStartComponent.class);
    }
    if (bombConfig.isIseStartBomb()) {
      return singletonList(ISEFailingStartComponent.class);
    }
    if (bombConfig.isOomStopBomb()) {
      return singletonList(OOMFailingStopComponent.class);
    }
    if (bombConfig.isIseStopBomb()) {
      return singletonList(ISEFailingStopComponent.class);
    }
    return emptyList();
  }

  @EagerStart
  public static final class OOMFailingStartComponent implements Startable {

    @Override
    public void start() {
      OOMGenerator.consumeAvailableMemory();
    }

    @Override
    public void stop() {
      // nothing to do
    }
  }

  @EagerStart
  public static final class ISEFailingStartComponent implements Startable {

    @Override
    public void start() {
      throw new IllegalStateException("Faking an IllegalStateException thrown by a startable component in the Analysis Report processing container");
    }

    @Override
    public void stop() {
      // nothing to do
    }
  }

  @EagerStart
  public static final class OOMFailingStopComponent implements Startable {

    @Override
    public void start() {
      // nothing to do
    }

    @Override
    public void stop() {
      OOMGenerator.consumeAvailableMemory();
    }
  }

  @EagerStart
  public static final class ISEFailingStopComponent implements Startable {

    @Override
    public void start() {
      // nothing to do
    }

    @Override
    public void stop() {
      throw new IllegalStateException("Faking an IllegalStateException thrown by a stoppable component in the Analysis Report processing container");
    }
  }
}
