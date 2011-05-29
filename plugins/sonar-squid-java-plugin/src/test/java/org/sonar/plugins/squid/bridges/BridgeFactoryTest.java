/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.squid.bridges;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class BridgeFactoryTest {

  @Test
  public void createForSourceAnalysis() {
    List<Bridge> astBridges = BridgeFactory.create(false, true, null, null, null, null, null);
    assertFalse(has(astBridges, DesignBridge.class));
    assertTrue(has(astBridges, CopyBasicMeasuresBridge.class));
  }

  @Test
  public void createForSourceAndBytecodeAnalysis() {
    List<Bridge> allBridges = BridgeFactory.create(true, false, null, null, null, null, null);
    assertTrue(has(allBridges, DesignBridge.class));
    assertTrue(has(allBridges, CopyBasicMeasuresBridge.class));
    assertTrue(has(allBridges, Lcom4BlocksBridge.class));
  }

  @Test
  public void createForSourceAndBytecodeWithoutDesignAnalysis() {
    List<Bridge> allBridges = BridgeFactory.create(true, true, null, null, null, null, null);
    assertFalse(has(allBridges, DesignBridge.class));
    assertTrue(has(allBridges, CopyBasicMeasuresBridge.class));
    assertTrue(has(allBridges, Lcom4BlocksBridge.class));
  }

  private boolean has(List<Bridge> bridges, Class<? extends Bridge> bridgeClass) {
    for (Bridge bridge : bridges) {
      if (bridge.getClass().equals(bridgeClass)) {
        return true;
      }
    }
    return false;
  }
}
