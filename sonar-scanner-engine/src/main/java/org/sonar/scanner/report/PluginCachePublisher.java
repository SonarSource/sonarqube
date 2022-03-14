/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.scanner.report;

import com.google.protobuf.ByteString;
import java.util.Map;
import org.sonar.scanner.cache.PluginCacheEnabled;
import org.sonar.scanner.cache.ScannerWriteCache;
import org.sonar.scanner.protocol.internal.ScannerInternal.PluginCacheMsg;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class PluginCachePublisher implements ReportPublisherStep {
  private final PluginCacheEnabled pluginCacheEnabled;
  private final ScannerWriteCache cache;

  public PluginCachePublisher(PluginCacheEnabled pluginCacheEnabled, ScannerWriteCache cache) {
    this.pluginCacheEnabled = pluginCacheEnabled;
    this.cache = cache;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    if (!pluginCacheEnabled.isEnabled() || cache.getCache().isEmpty()) {
      return;
    }
    PluginCacheMsg.Builder pluginCacheMsg = PluginCacheMsg.newBuilder();

    for (Map.Entry<String, byte[]> entry : cache.getCache().entrySet()) {
      pluginCacheMsg.putMap(entry.getKey(), ByteString.copyFrom(entry.getValue()));
    }

    writer.writePluginCache(pluginCacheMsg.build());
  }
}
