/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.tests.volume;

import com.thoughtworks.selenium.DefaultSelenium;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HomepageTest {
  private DefaultSelenium selenium;

  @Before
  public void before() throws Exception {
    selenium = new DefaultSelenium("localhost", 4444, "*firefox", "http://localhost:9000");
    selenium.start();
  }

  @After
  public void after() {
    selenium.stop();
  }

  @Test
  public void testHomepage() throws Exception {
    selenium.open("/");
    selenium.waitForPageToLoad("30000");
    assertTrue(selenium.isElementPresent("results"));
  }

}
