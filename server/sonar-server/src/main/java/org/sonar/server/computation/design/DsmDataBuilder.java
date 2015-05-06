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

package org.sonar.server.computation.design;

import org.sonar.graph.Dsm;
import org.sonar.graph.DsmCell;
import org.sonar.server.computation.step.ComponentUuidsCache;
import org.sonar.server.design.db.DsmDb;

public final class DsmDataBuilder {

  private final Dsm<Integer> dsm;
  private final ComponentUuidsCache uuidByRef;

  private final DsmDb.Data.Builder dsmBuilder;

  private DsmDataBuilder(Dsm<Integer> dsm, ComponentUuidsCache uuidByRef) {
    this.dsm = dsm;
    this.uuidByRef = uuidByRef;
    this.dsmBuilder = DsmDb.Data.newBuilder();
  }

  private DsmDb.Data build() {
    processRows();
    return dsmBuilder.build();
  }

  private void processRows() {
    for (int y = 0; y < dsm.getDimension(); y++) {
      processRow(y);
    }
  }

  private void processRow(int y) {
    Integer ref = dsm.getVertex(y);
    String uuid = uuidByRef.getUuidFromRef(ref);
    if (uuid == null) {
      throw new IllegalArgumentException(String.format("Reference '%s' has no associate uuid", ref));
    }
    dsmBuilder.addUuid(uuid);
    for (int x = 0; x < dsm.getDimension(); x++) {
      processCell(y, x);
    }
  }

  private void processCell(int y, int x) {
    DsmCell cell = dsm.cell(x, y);
    if (cell != null && cell.getEdge() != null && cell.getWeight() > 0) {
      dsmBuilder.addCellBuilder()
        .setOffset(dsm.getDimension() * x + y)
        .setWeight(cell.getWeight())
        .build();
    }
  }

  public static DsmDb.Data build(Dsm<Integer> dsm, ComponentUuidsCache uuidByRef) {
    return new DsmDataBuilder(dsm, uuidByRef).build();
  }
}
