/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.measure;

import java.util.List;
import javax.annotation.CheckForNull;

import static java.lang.String.format;

public class SearchHistoryRequest {
  public static final int MAX_PAGE_SIZE = 1_000;
  public static final int DEFAULT_PAGE_SIZE = 100;

  private final String component;
  private final List<String> metrics;
  private final String from;
  private final String to;
  private final int page;
  private final int pageSize;

  public SearchHistoryRequest(Builder builder) {
    this.component = builder.component;
    this.metrics = builder.metrics;
    this.from = builder.from;
    this.to = builder.to;
    this.page = builder.page;
    this.pageSize = builder.pageSize;
  }

  public String getComponent() {
    return component;
  }

  public List<String> getMetrics() {
    return metrics;
  }

  @CheckForNull
  public String getFrom() {
    return from;
  }

  @CheckForNull
  public String getTo() {
    return to;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String component;
    private List<String> metrics;
    private String from;
    private String to;
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
      // enforce build factory method
    }

    public Builder setComponent(String component) {
      this.component = component;
      return this;
    }

    public Builder setMetrics(List<String> metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder setFrom(String from) {
      this.from = from;
      return this;
    }

    public Builder setTo(String to) {
      this.to = to;
      return this;
    }

    public Builder setPage(int page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public SearchHistoryRequest build() {
      checkArgument(component != null && !component.isEmpty(), "Component key is required");
      checkArgument(metrics != null && !metrics.isEmpty(), "Metric keys are required");
      checkArgument(pageSize <= MAX_PAGE_SIZE, "Page size (%d) must be lower than or equal to %d", pageSize, MAX_PAGE_SIZE);

      return new SearchHistoryRequest(this);
    }

    private static void checkArgument(boolean condition, String message, Object... args) {
      if (!condition) {
        throw new IllegalArgumentException(format(message, args));
      }
    }
  }
}
