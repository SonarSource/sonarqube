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
package org.sonar.ce.task.projectanalysis.api.posttask;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

@Immutable
class ScannerContextImpl implements ScannerContext {

  private final Map<String, String> props;

  private ScannerContextImpl(Map<String, String> props) {
    this.props = props;
  }

  @Override
  public Map<String, String> getProperties() {
    return props;
  }

  static ScannerContextImpl from(CloseableIterator<ScannerReport.ContextProperty> it) {
    try {
      ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
      while (it.hasNext()) {
        ScannerReport.ContextProperty prop = it.next();
        mapBuilder.put(prop.getKey(), prop.getValue());
      }
      return new ScannerContextImpl(mapBuilder.build());
    } finally {
      it.close();
    }
  }
}
