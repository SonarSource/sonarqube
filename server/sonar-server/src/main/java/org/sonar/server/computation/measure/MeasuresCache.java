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

package org.sonar.server.computation.measure;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

/**
 * Cache of all measures involved in the analysis.
 * TODO Use a cache on disk
 */
public class MeasuresCache {

  private Multimap<Integer, Measure> measuresByRef;

  public MeasuresCache() {
    this.measuresByRef = ArrayListMultimap.create();
  }

  public void addMeasure(int ref, Measure measure) {
    measuresByRef.put(ref, measure);
  }

  public Collection<Measure> getMeasures(int ref) {
    return measuresByRef.get(ref);
  }

}
