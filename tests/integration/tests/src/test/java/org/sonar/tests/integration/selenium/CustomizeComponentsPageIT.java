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
package org.sonar.tests.integration.selenium;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CustomizeComponentsPageIT extends SonarTestCase {

  @Test
  public void defaultTreemapIsCustomizableByAdministrators() {
    loginAsAdministrator();
    selenium.open("/components/index/org.apache.struts:struts-parent");

    // configure is OFF
    assertFalse(selenium.isElementPresent("set_default_treemap"));

    // configure is ON
    clickAndWait("configure-on");    
    assertTrue(selenium.isElementPresent("set_default_treemap"));
  }

  @Test
  public void notCustomizableByAnonymous() {
    loginAsAnonymous();
    selenium.open("/components/index/org.apache.struts:struts-parent");
    assertFalse(selenium.isElementPresent("configure-on"));
  }
}
