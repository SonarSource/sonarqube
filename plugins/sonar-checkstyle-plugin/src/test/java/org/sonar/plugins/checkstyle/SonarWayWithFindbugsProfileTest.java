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
package org.sonar.plugins.checkstyle;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.profiles.ProfilePrototype;
import org.sonar.api.utils.ValidationMessages;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class SonarWayWithFindbugsProfileTest {

  @Test
  public void sameAsSonarWay() {
    ProfilePrototype withFindbugs = new SonarWayWithFindbugsProfile().createPrototype(ValidationMessages.create());
    ProfilePrototype withoutFindbugs = new SonarWayProfile().createPrototype(ValidationMessages.create());
    assertThat(withFindbugs.getRules().size(), is(withoutFindbugs.getRules().size()));
  }
}
