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

import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.squid.Squid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BridgeFactory {

  private BridgeFactory() {
    // only static methods
  }

  private static List<Bridge> create(NoSonarFilter noSonarFilter) {
    return Arrays.asList(new CopyBasicMeasuresBridge(), new PackagesBridge(), new PublicUndocumentedApiBridge(),
        new NoSonarFilterLoader(noSonarFilter),
        new ChidamberKemererBridge(), new RobertCMartinBridge(), new DesignBridge(),
        new Lcom4BlocksBridge(), new ChecksBridge());
  }

  public static List<Bridge> create(boolean bytecodeScanned, SensorContext context, CheckFactory checkFactory,
                                    ResourceIndex resourceIndex, Squid squid, NoSonarFilter noSonarFilter) {
    List<Bridge> result = new ArrayList<Bridge>();
    for (Bridge bridge : create(noSonarFilter)) {
      bridge.setCheckFactory(checkFactory);
      if (!bridge.needsBytecode() || bytecodeScanned) {
        bridge.setContext(context);
        bridge.setSquid(squid);
        bridge.setResourceIndex(resourceIndex);
        result.add(bridge);
      }
    }
    return result;
  }
}
