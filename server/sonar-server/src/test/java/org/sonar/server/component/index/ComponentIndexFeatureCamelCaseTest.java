/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component.index;

import org.junit.Before;
import org.junit.Test;

public class ComponentIndexFeatureCamelCaseTest extends ComponentIndexTest {

  @Before
  public void before() {
    features.set(ComponentIndexSearchFeature.CAMEL_CASE);
  }

  @Test
  public void HW_should_find_HelloWorld() {
    assertFileMatches("HW", "HelloWorld");
  }

  @Test
  public void NPE_should_find_NullPointerException() {
    assertFileMatches("NPE", "NullPointerException.java");
  }

  @Test
  public void npe_should_not_find_NullPointerException() {
    assertNoFileMatches("npe", "NullPointerException.java");
  }

  @Test
  public void NuPE_should_find_NullPointerException() {
    assertFileMatches("NuPE", "NullPointerException.java");
  }

  @Test
  public void NPoE_should_find_NullPointerException() {
    assertFileMatches("NPoE", "NullPointerException.java");
  }

  @Test
  public void NPEx_should_find_NullPointerException() {
    assertFileMatches("NPEx", "NullPointerException.java");
  }

  @Test
  public void PE_should_prefer_PointerException_to_NullPointException() {
    assertResultOrder("PE", "PointerException.java", "NullPointerException.java");
  }

  @Test
  public void HContainer_should_prefer_HomeContainer_to_MeasureHistoryContainer() {
    assertResultOrder("HContainer", "HomeContainer.js", "MeasureHistoryContainer");
  }

  @Test
  public void should_respect_order_of_camel_case_words() {
    assertNoFileMatches("NuExcPo", "NullPointerException.java");
  }

  @Test
  public void should_take_all_characters_into_account_when_doing_camel_case_search() {
    assertNoFileMatches("MCooooooo", "MeasureCache.java");
    assertFileMatches("MCo", "MigrationContainer.java");
    assertNoFileMatches("MCooooooo", "MigrationContainer.java");
  }
}
