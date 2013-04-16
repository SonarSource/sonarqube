/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.profiling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractTimeProfiling {

  private final long startTime;

  private long totalTime;

  public AbstractTimeProfiling() {
    this.startTime = System.currentTimeMillis();
  }

  public long startTime() {
    return startTime;
  }

  public void stop() {
    this.totalTime = System.currentTimeMillis() - startTime;
  }

  public long totalTime() {
    return totalTime;
  }

  public String totalTimeAsString() {
    if (totalTime < 1000) {
      return String.format("%sms", totalTime);
    }
    else {
      long sec = totalTime / 1000;
      // long remainingMs = totalTime - (sec * 1000);
      if (sec < 60) {
        return String.format("%ss", sec);
      }
      else {
        long min = sec / 60;
        long remainingSec = sec - (min * 60);
        if (remainingSec > 0) {
          return String.format("%smin %ss", min, remainingSec);
        }
        else {
          return String.format("%smin", min);
        }
      }
    }
  }

  public void setTotalTime(long totalTime) {
    this.totalTime = totalTime;
  }

  protected void add(AbstractTimeProfiling other) {
    this.setTotalTime(this.totalTime() + other.totalTime());
  }

  protected <G extends AbstractTimeProfiling> List<G> sortByDescendingTotalTime(Collection<G> unsorted) {
    List<G> result = new ArrayList<G>(unsorted.size());
    result.addAll(unsorted);
    Collections.sort(result, new Comparator<G>() {
      @Override
      public int compare(G o1, G o2) {
        return Long.valueOf(o2.totalTime()).compareTo(o1.totalTime());
      }
    });
    return result;
  }

}
