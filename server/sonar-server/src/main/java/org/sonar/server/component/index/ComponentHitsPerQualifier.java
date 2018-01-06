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
package org.sonar.server.component.index;

import java.util.List;

public class ComponentHitsPerQualifier {

  private final String qualifier;
  private final List<ComponentHit> hits;
  private final long totalHits;

  public ComponentHitsPerQualifier(String qualifier, List<ComponentHit> hits, long totalHits) {
    this.qualifier = qualifier;
    this.hits = hits;
    this.totalHits = totalHits;
  }

  public String getQualifier() {
    return qualifier;
  }

  public List<ComponentHit> getHits() {
    return hits;
  }

  public long getTotalHits() {
    return totalHits;
  }
}
