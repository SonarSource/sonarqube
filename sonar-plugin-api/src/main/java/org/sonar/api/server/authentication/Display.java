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
package org.sonar.api.server.authentication;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.api.utils.Preconditions.checkArgument;

/**
 * Display information provided by the Identity Provider to be displayed into the login form.
 *
 * @since 5.4
 */
@Immutable
public final class Display {

  private final String iconPath;
  private final String backgroundColor;
  private final String helpMessage;

  private Display(Builder builder) {
    this.iconPath = builder.iconPath;
    this.backgroundColor = builder.backgroundColor;
    this.helpMessage = builder.helpMessage;
  }

  /**
   * URL path to the provider icon, as deployed at runtime, for example "/static/authgithub/github.svg" (in this
   * case "authgithub" is the plugin key. Source file is "src/main/resources/static/github.svg").
   * It can also be an external URL, for example "http://www.mydomain/myincon.png".
   * Must not be blank.
   * <br>
   * The recommended format is SVG with a size of 24x24 pixels.
   * Other supported format is PNG, with a size of 40x40 pixels.
   */
  public String getIconPath() {
    return iconPath;
  }

  /**
   * Background color for the provider button displayed in the login form.
   * It's a Hexadecimal value, for instance #205081.
   * <br>
   * If not provided, the default value is #236a97
   */
  public String getBackgroundColor() {
    return backgroundColor;
  }

  /**
   * Optional help message to be displayed near the provider button.
   */
  @CheckForNull
  public String getHelpMessage() {
    return helpMessage;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String iconPath;
    private String backgroundColor = "#236a97";
    private String helpMessage;

    private Builder() {
    }

    /**
     * @see Display#getIconPath()
     */
    public Builder setIconPath(String iconPath) {
      this.iconPath = iconPath;
      return this;
    }

    /**
     * @see Display#getBackgroundColor()
     */
    public Builder setBackgroundColor(String backgroundColor) {
      this.backgroundColor = backgroundColor;
      return this;
    }

    /**
     * @see Display#getHelpMessage()
     */
    public Builder setHelpMessage(@CheckForNull String helpMessage) {
      this.helpMessage = helpMessage;
      return this;
    }

    public Display build() {
      checkArgument(isNotBlank(iconPath), "Icon path must not be blank");
      validateBackgroundColor();
      return new Display(this);
    }

    private void validateBackgroundColor() {
      checkArgument(isNotBlank(backgroundColor), "Background color must not be blank");
      checkArgument(backgroundColor.length() == 7 && backgroundColor.indexOf('#') == 0,
        "Background color must begin with a sharp followed by 6 characters");
    }
  }
}
