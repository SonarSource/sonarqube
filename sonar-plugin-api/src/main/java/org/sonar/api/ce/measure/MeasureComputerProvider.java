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

package org.sonar.api.ce.measure;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * This extension point can be used to register {@link MeasureComputer}(s) that will be able to compute measures when a batch report is processed by the Compute Engine
 */
@ServerSide
@ExtensionPoint
public interface MeasureComputerProvider {

  /**
   *  Use this method to register a new measure computer.
   */
  void register(Context ctx);

  interface Context {

    /**
     * Add a new computer to the context.
     *
     * @throws UnsupportedOperationException when trying to add a computer providing some measures on metrics already defined by another {@link MeasureComputer}
     */
    Context add(MeasureComputer measureComputer);

    /**
     * Use this method to build a MeasureComputer to be used in the {@link #add(MeasureComputer)} method
     */
    MeasureComputer.MeasureComputerBuilder newMeasureComputerBuilder();

  }
}
