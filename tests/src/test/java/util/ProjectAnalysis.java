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
package util;

public interface ProjectAnalysis {
  /**
   * Creates a new ProjectAnalysis which will use the specified quality profile.
   *
   * @throws IllegalArgumentException if the quality profile with the specified key has not been loaded into the Rule
   * @see {@link ProjectAnalysisRule#registerProfile(String)}
   */
  ProjectAnalysis withQualityProfile(String qualityProfileKey);

  /**
   * Creates a new ProjectAnalysis which will use the built-in Xoo empty profile.
   */
  ProjectAnalysis withXooEmptyProfile();

  /**
   * Creates a new ProjectAnalysis which will have debug logs enabled (or not).
   */
  ProjectAnalysis withDebugLogs(boolean enabled);

  /**
   * Creates a new ProjectAnalysis which will have the specified properties.
   */
  ProjectAnalysis withProperties(String... properties);

  /**
   * Execute the current ProjectAnalysis.
   * This method can be called any number of time and will run the same analysis again and again.
   */
  void run();
}
