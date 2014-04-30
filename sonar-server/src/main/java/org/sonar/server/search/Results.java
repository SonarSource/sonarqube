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
package org.sonar.server.search;

import java.util.ArrayList;
import java.util.Collection;

public class Results {

  private Collection<Hit> hits;

  private int total;

  private int offset;

  private long time;

  public Results() {
    this.hits = new ArrayList<Hit>();
  }

  public Collection<Hit> getHits() {
    return hits;
  }

  public int getTotal() {
    return total;
  }

  public int getOffset() {
    return offset;
  }

  public Results setTotal(int total) {
    this.total = total;
    return this;
  }

  public Results setOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public long getTime() {
    return time;
  }

  public Results setTime(long time) {
    this.time = time;
    return this;
  }
}
