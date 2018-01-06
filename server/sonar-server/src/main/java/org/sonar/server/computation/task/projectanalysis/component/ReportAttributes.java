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

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Component properties which are specific to the Batch Report.
 */
@Immutable
public class ReportAttributes {
  private final int ref;
  @CheckForNull
  private final String version;
  @CheckForNull
  private final String path;
  @CheckForNull
  private final String scmPath;

  private ReportAttributes(Builder builder) {
    this.ref = builder.ref;
    this.version = builder.version;
    this.path = builder.path;
    this.scmPath = builder.scmPath;
  }

  public static Builder newBuilder(int ref) {
    return new Builder(ref);
  }

  public static class Builder {
    private final int ref;
    @CheckForNull
    private String version;
    @CheckForNull
    private String path;
    @CheckForNull
    private String scmPath;

    private Builder(int ref) {
      this.ref = ref;
    }

    public Builder setVersion(@Nullable String version) {
      this.version = version;
      return this;
    }

    public Builder setPath(@Nullable String path) {
      this.path = path;
      return this;
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
   */
  public int getRef() {
    return ref;
  }

  /**
   * The project or module version as defined in the batch report.
   */
  @CheckForNull
  public String getVersion() {
    return this.version;
  }

  /**
   * The path of the report component, must be non null for module, directories and files.
   */
  @CheckForNull
  public String getPath() {
    return path;
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
      ", version='" + version + '\'' +
      ", path='" + path + '\'' +
      ", scmPath='" + scmPath + '\'' +
      '}';
  }
}
