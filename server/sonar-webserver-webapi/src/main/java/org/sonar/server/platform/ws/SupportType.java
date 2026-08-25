/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.ws;

/**
 * Known values for the support type field exposed by the billing squad via {@code License#getSupportType()}.
 * Update this class when a new support tier is introduced.
 * {@code STANDARD} and {@code CORE} are documented here for completeness but are not actively read by the current code.
 */
public final class SupportType {

  public static final String STANDARD = "standard";
  public static final String PREMIUM = "premium";
  public static final String CORE = "core";

  private SupportType() {
    // constants only
  }
}
