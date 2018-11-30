/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Component properties which are specific to the Scanner Report.
 */
@Immutable
public class ReportAttributes {
  @CheckForNull
  private final Integer ref;
  @CheckForNull
  private final String scmPath;

  private ReportAttributes(Builder builder) {
    this.ref = builder.ref;
    this.scmPath = builder.scmPath;
  }

  public static Builder newBuilder(@Nullable Integer ref) {
    return new Builder(ref);
  }

  public static class Builder {
    @CheckForNull
    private final Integer ref;
    @CheckForNull
    private String scmPath;

    private Builder(@Nullable Integer ref) {
      this.ref = ref;
    }

    public Builder setScmPath(@Nullable String scmPath) {
      this.scmPath = scmPath;
      return this;
    }

    public ReportAttributes build() {
      return new ReportAttributes(this);
    }
  }

  /**
   * The component ref in the batch report.
   * Will be null for directories.
   */
  @CheckForNull
  public Integer getRef() {
    return ref;
  }

  /**
   * The path of the component relative the SCM root the project is part of.
   * <p>
   * Can be {@link Optional#empty() empty} if project is not version controlled,
   * otherwise should be non {@link Optional#isPresent() non empty} for all components.
   */
  public Optional<String> getScmPath() {
    return Optional.ofNullable(scmPath);
  }

  @Override
  public String toString() {
    return "ReportAttributes{" +
      "ref=" + ref +
      ", scmPath='" + scmPath + '\'' +
      '}';
  }
}
