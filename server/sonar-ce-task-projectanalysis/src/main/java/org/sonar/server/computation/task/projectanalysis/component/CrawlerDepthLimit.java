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
package org.sonar.server.computation.task.projectanalysis.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents the limit down a {@link Component} tree a {@link ComponentCrawler} can go to.
 *
 * A limit can be defined for a tree of Report components, a tree of Views components or both.
 *
 * Constants are provided for limits specific to a component tree (see {@link #PROJECT}, {@link #MODULE}, etc.).
 *
 * Limits for both trees can be created using the {@link #reportMaxDepth(Component.Type)} static method.
 */
@Immutable
public class CrawlerDepthLimit {
  private static final String UNSUPPORTED_TYPE_UOE_MSG = "Specified type is neither a report type nor a views type";

  public static final CrawlerDepthLimit PROJECT = new CrawlerDepthLimit(Component.Type.PROJECT, null);
  public static final CrawlerDepthLimit MODULE = new CrawlerDepthLimit(Component.Type.MODULE, null);
  public static final CrawlerDepthLimit DIRECTORY = new CrawlerDepthLimit(Component.Type.DIRECTORY, null);
  public static final CrawlerDepthLimit FILE = new CrawlerDepthLimit(Component.Type.FILE, null);
  public static final CrawlerDepthLimit VIEW = new CrawlerDepthLimit(null, Component.Type.VIEW);
  public static final CrawlerDepthLimit SUBVIEW = new CrawlerDepthLimit(null, Component.Type.SUBVIEW);
  public static final CrawlerDepthLimit PROJECT_VIEW = new CrawlerDepthLimit(null, Component.Type.PROJECT_VIEW);
  public static final CrawlerDepthLimit LEAVES = new CrawlerDepthLimit(Component.Type.FILE, Component.Type.PROJECT_VIEW);
  public static final CrawlerDepthLimit ROOTS = new CrawlerDepthLimit(Component.Type.PROJECT, Component.Type.VIEW);

  @CheckForNull
  private final Component.Type reportMaxDepth;
  @CheckForNull
  private final Component.Type viewsMaxDepth;

  private CrawlerDepthLimit(@Nullable Component.Type reportMaxDepth, @Nullable Component.Type viewsMaxDepth) {
    checkArgument(reportMaxDepth != null || viewsMaxDepth != null,
        "At least one type must be non null");
    checkArgument(reportMaxDepth == null || reportMaxDepth.isReportType());
    checkArgument(viewsMaxDepth == null || viewsMaxDepth.isViewsType());
    this.reportMaxDepth = reportMaxDepth;
    this.viewsMaxDepth = viewsMaxDepth;
  }

  public static Builder reportMaxDepth(Component.Type reportMaxDepth) {
    return new Builder(reportMaxDepth);
  }

  public static class Builder {
    private final Component.Type reportMaxDepth;

    public Builder(Component.Type reportMaxDepth) {
      checkArgument(reportMaxDepth.isReportType(), "A Report max depth must be a report type");
      this.reportMaxDepth = reportMaxDepth;
    }

    public CrawlerDepthLimit withViewsMaxDepth(Component.Type viewsMaxDepth) {
      checkArgument(viewsMaxDepth.isViewsType(), "A Views max depth must be a views type");
      return new CrawlerDepthLimit(reportMaxDepth, viewsMaxDepth);
    }
  }

  public boolean isDeeperThan(Component.Type otherType) {
    if (otherType.isViewsType()) {
      return this.viewsMaxDepth != null && this.viewsMaxDepth.isDeeperThan(otherType);
    }
    if (otherType.isReportType()) {
      return this.reportMaxDepth != null && this.reportMaxDepth.isDeeperThan(otherType);
    }
    throw new UnsupportedOperationException(UNSUPPORTED_TYPE_UOE_MSG);
  }

  public boolean isHigherThan(Component.Type otherType) {
    if (otherType.isViewsType()) {
      return this.viewsMaxDepth != null && this.viewsMaxDepth.isHigherThan(otherType);
    }
    if (otherType.isReportType()) {
      return this.reportMaxDepth != null && this.reportMaxDepth.isHigherThan(otherType);
    }
    throw new UnsupportedOperationException(UNSUPPORTED_TYPE_UOE_MSG);
  }

  public boolean isSameAs(Component.Type otherType) {
    if (otherType.isViewsType()) {
      return otherType == this.viewsMaxDepth;
    }
    if (otherType.isReportType()) {
      return otherType == this.reportMaxDepth;
    }
    throw new UnsupportedOperationException(UNSUPPORTED_TYPE_UOE_MSG);
  }

}
