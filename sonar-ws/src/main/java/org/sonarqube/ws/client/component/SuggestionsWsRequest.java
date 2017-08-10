/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.component;

import java.util.Collections;
import java.util.List;

public class SuggestionsWsRequest {

  public static final int MAX_PAGE_SIZE = 500;
  public static final int DEFAULT_PAGE_SIZE = 100;

  public enum More {
    VW,
    SVW,
    APP,
    TRK,
    BRC,
    FIL,
    UTS
  }

  private final More more;
  private final List<String> recentlyBrowsed;
  private final String s;

  private SuggestionsWsRequest(Builder builder) {
    this.more = builder.more;
    this.recentlyBrowsed = builder.recentlyBrowsed;
    this.s = builder.s;
  }

  public More getMore() {
    return more;
  }

  public List<String> getRecentlyBrowsed() {
    return recentlyBrowsed;
  }

  public String getS() {
    return s;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private More more;
    private List<String> recentlyBrowsed = Collections.emptyList();
    private String s;

    private Builder() {
      // enforce static factory method
    }

    public Builder setMore(More more) {
      this.more = more;
      return this;
    }

    public Builder setRecentlyBrowsed(List<String> recentlyBrowsed) {
      this.recentlyBrowsed = recentlyBrowsed;
      return this;
    }

    public Builder setS(String s) {
      this.s = s;
      return this;
    }

    public SuggestionsWsRequest build() {
      return new SuggestionsWsRequest(this);
    }
  }
}
