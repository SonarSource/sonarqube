/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Iso9126RulesCategories {
  private Iso9126RulesCategories() {
  }

  public static final RulesCategory RELIABILITY = new RulesCategory("Reliability", "The extent to which the project can be expected to perform its intended function with rescission. Some examples : are loop indexes range tested? Is input data checked for range errors ? Is divide-by-zero avoided ? Is exception handling provided ?");
  public static final RulesCategory EFFICIENCY = new RulesCategory("Efficiency", "The extent to which the project fulfills its purpose without waste of resources. This means resources in the sense of memory utilisation and processor speed.");
  public static final RulesCategory PORTABILITY = new RulesCategory("Portability", "The extent to which the project can be operated easily and well on multiple computer configurations. Portability can mean both between different hardware setups and between different operating systems -- such as running on both Mac OS X and GNU/Linux.");
  public static final RulesCategory USABILITY = new RulesCategory("Usability", "The extent to which the project can be understood, learned, operated, attractive and compliant with usability regulations and guidelines. It commonly relies on naming conventions and formatting rules.");
  public static final RulesCategory MAINTAINABILITY = new RulesCategory("Maintainability", "The extent to which the project facilitates updating to satisfy new requirements. Thus the the project which is maintainable should be not complex.");

  public static final List<RulesCategory> ALL = Collections.unmodifiableList(Arrays.asList(RELIABILITY, EFFICIENCY, PORTABILITY, USABILITY, MAINTAINABILITY));
}
