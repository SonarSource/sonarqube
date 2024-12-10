/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.alm.client.bitbucketserver;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProjectList {

  @SerializedName("isLastPage")
  private boolean lastPage;

  @SerializedName("nextPageStart")
  private int nextPageStart;

  @SerializedName("size")
  private int size;

  @SerializedName("values")
  private List<Project> values;

  private ProjectList() {
    // http://stackoverflow.com/a/18645370/229031
    this(true, 0, 0, List.of());
  }

  public ProjectList(boolean lastPage, int nextPageStart, int size, List<Project> values) {
    this.lastPage = lastPage;
    this.nextPageStart = nextPageStart;
    this.size = size;
    this.values = values;
  }

  public boolean isLastPage() {
    return lastPage;
  }

  public int getNextPageStart() {
    return nextPageStart;
  }

  public int getSize() {
    return size;
  }

  public List<Project> getValues() {
    return values;
  }

  @Override
  public String toString() {
    return "{isLastPage=%s, nextPageStart=%s, size=%s, values=%s}"
      .formatted(lastPage, nextPageStart, size, values);
  }

}
