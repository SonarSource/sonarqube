/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class RepositoryList {

  @SerializedName("isLastPage")
  private boolean isLastPage;

  @SerializedName("values")
  private List<Repository> values;

  public RepositoryList() {
    // http://stackoverflow.com/a/18645370/229031
    this(false, new ArrayList<>());
  }

  public RepositoryList(boolean isLastPage, List<Repository> values) {
    this.isLastPage = isLastPage;
    this.values = values;
  }

  static RepositoryList parse(String json) {
    return new Gson().fromJson(json, RepositoryList.class);
  }

  public boolean isLastPage() {
    return isLastPage;
  }

  public RepositoryList setLastPage(boolean lastPage) {
    isLastPage = lastPage;
    return this;
  }

  public List<Repository> getValues() {
    return values;
  }

  public RepositoryList setValues(List<Repository> values) {
    this.values = values;
    return this;
  }

  @Override
  public String toString() {
    return "{" +
      "isLastPage=" + isLastPage +
      ", values=" + values +
      '}';
  }
}
