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

package org.sonar.wsclient.test.internal;

import org.sonar.wsclient.test.Coverage;

import java.util.List;

public class DefaultCoverage implements Coverage {

  private final List json;

  public DefaultCoverage(List json) {
    this.json = json;
  }

  @Override
  public Integer lineIndex() {
    Object value = json.get(0);
    return value != null ? ((Long) value).intValue() : null;
  }

  @Override
  public Boolean isCovered() {
    Object value = json.get(1);
    return value != null ? (Boolean) value : null;
  }

  @Override
  public Integer tests() {
    Object value = json.get(2);
    return value != null ? ((Long) value).intValue() : null;
  }

  @Override
  public Integer branches() {
    Object value = json.get(3);
    return value != null ? ((Long) value).intValue() : null;
  }

  @Override
  public Integer coveredBranches() {
    Object value = json.get(4);
    return value != null ? ((Long) value).intValue() : null;
  }
}
