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
package org.sonar.api.resources;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;

/**
 * Experimental extension to declare types of resources.
 *
 * @since 2.14
 */
@Beta
public final class ResourceDefinition implements BatchExtension, ServerExtension {

  public static class Builder {
    private String qualifier;
    private String iconPath;
    private boolean availableForFilters = false;
    private boolean hasSourceCode = false;

    public Builder(String qualifier) {
      this.qualifier = qualifier;
    }

    /**
     * @param iconPath path to icon, relative to context of web-application (e.g. "/images/q/DIR.png")
     */
    public Builder setIconPath(String iconPath) {
      this.iconPath = iconPath;
      return this;
    }

    public Builder availableForFilters() {
      this.availableForFilters = true;
      return this;
    }

    public Builder hasSourceCode() {
      this.hasSourceCode = true;
      return this;
    }

    public ResourceDefinition build() {
      if (Strings.isNullOrEmpty(iconPath)) {
        iconPath = "/images/q/" + qualifier + ".png";
      }
      return new ResourceDefinition(this);
    }
  }

  public static Builder builder(String qualifier) {
    Preconditions.checkNotNull(qualifier);
    Preconditions.checkArgument(qualifier.length() <= 10, "Qualifier is limited to 10 characters in database");
    return new Builder(qualifier);
  }

  private final String qualifier;
  private final String iconPath;
  private final boolean hasSourceCode;
  private final boolean availableForFilters;

  private ResourceDefinition(Builder builder) {
    this.qualifier = builder.qualifier;
    this.iconPath = builder.iconPath;
    this.availableForFilters = builder.availableForFilters;
    this.hasSourceCode = builder.hasSourceCode;
  }

  public String getQualifier() {
    return qualifier;
  }

  public String getIconPath() {
    return iconPath;
  }

  public boolean isAvailableForFilters() {
    return availableForFilters;
  }

  public boolean hasSourceCode() {
    return hasSourceCode;
  }
}
