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
package org.sonar.api.resources;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * <p>Experimental extension to declare types of resources.
 * <p>
 * Since 3.0, ResourceType object can declare properties that give information about the capabilities of the
 * resource type. Those properties may be used, of instance, to adapt the Web UI according to the type of
 * the resource being displayed.
 * <br>
 * Currently, the following properties can be defined:
 * 
 * <ul>
 * <li>"deletable": if set to "true", then this resource can be deleted/purged.</li>
 * <li>"supportsMeasureFilters": if set to "true", then this resource can be displayed in measure filters</li>
 * <li>"modifiable_history": if set to "true", then the history of this resource may be modified (deletion of snapshots, modification of events, ...)</li>
 * <li>"updatable_key" (since 3.2): if set to "true", then it is possible to update the key of this resource</li>
 * <li>"supportsGlobalDashboards" (since 3.2): if true, this resource can be displayed in global dashboards</li>
 * <li>"hasRolePolicy" : if true, roles configuration is available in sidebar</li>
 * <li>"comparable" (since 3.4) : if true, the resource can be compared to other resources</li>
 * <li>"configurable" (since 3.6) : if true, the settings page can be displayed on the resource</li>
 * </ul>
 *
 * @since 2.14
 */
@Immutable
public class ResourceType {

  private final String qualifier;
  private final String iconPath;
  private final boolean hasSourceCode;
  private Map<String, String> properties;

  private ResourceType(Builder builder) {
    this.qualifier = builder.qualifier;
    this.iconPath = builder.iconPath;
    this.hasSourceCode = builder.hasSourceCode;
    this.properties = new HashMap<>(builder.properties);
  }

  /**
   * Qualifier is the unique key.
   *
   * @return the qualifier
   */
  public String getQualifier() {
    return qualifier;
  }

  /**
   * Returns the relative path of the icon used to represent the resource type
   *
   * @return the relative path.
   */
  public String getIconPath() {
    return iconPath;
  }

  /**
   * Tells whether resources of this type has source code or not.
   *
   * @return true if the type has source code
   */
  public boolean hasSourceCode() {
    return hasSourceCode;
  }

  public boolean hasProperty(String key) {
    requireNonNull(key);
    return properties.containsKey(key);
  }

  /**
   * Returns the value of the property for this resource type.
   *
   * @return the String value of the property, or NULL if the property hasn't been set.
   * @since 3.0
   */
  public String getStringProperty(String key) {
    requireNonNull(key);
    return properties.get(key);
  }

  /**
   * Returns the value of the property for this resource type.
   *
   * @return the Boolean value of the property. If the property hasn't been set, False is returned.
   * @since 3.0
   */
  public boolean getBooleanProperty(String key) {
    requireNonNull(key);
    String value = properties.get(key);
    return value != null && Boolean.parseBoolean(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResourceType that = (ResourceType) o;
    return qualifier.equals(that.qualifier);
  }

  @Override
  public int hashCode() {
    return qualifier.hashCode();
  }

  @Override
  public String toString() {
    return qualifier;
  }

  /**
   * Creates a new {@link Builder}
   */
  public static Builder builder(String qualifier) {
    requireNonNull(qualifier);
    Preconditions.checkArgument(qualifier.length() <= 10, "Qualifier is limited to 10 characters");
    return new Builder(qualifier);
  }

  /**
   * Builder used to create {@link ResourceType} objects.
   */
  public static class Builder {
    private String qualifier;
    private String iconPath;
    private boolean hasSourceCode = false;
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Creates a new {@link Builder}
     */
    public Builder(String qualifier) {
      this.qualifier = qualifier;
    }

    /**
     * Relative path of the icon used to represent the resource type.
     *
     * @param iconPath path to icon, relative to context of web-application (e.g. "/images/q/DIR.png")
     */
    public Builder setIconPath(@Nullable String iconPath) {
      this.iconPath = iconPath;
      return this;
    }

    /**
     * @deprecated since 3.0. Use {@link #setProperty(String, String)} with "supportsMeasureFilters" set to "true".
     */
    @Deprecated
    public Builder availableForFilters() {
      return this;
    }

    /**
     * Tells that the resources of this type will have source code.
     */
    public Builder hasSourceCode() {
      this.hasSourceCode = true;
      return this;
    }

    /**
     * Sets a property on the resource type. See the description of {@link ResourceType} class for more information.
     *
     * @since 3.0
     */
    public Builder setProperty(String key, String value) {
      requireNonNull(key);
      requireNonNull(value);
      properties.put(key, value);
      return this;
    }

    /**
     * @since 3.2
     */
    public Builder setProperty(String key, boolean value) {
      return setProperty(key, String.valueOf(value));
    }

    /**
     * Creates an instance of {@link ResourceType} based on all information given to the builder.
     */
    public ResourceType build() {
      if (isEmpty(iconPath)) {
        iconPath = "/images/q/" + qualifier + ".png";
      }
      return new ResourceType(this);
    }
  }
}
