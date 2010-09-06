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

import com.thoughtworks.selenium.DefaultSelenium;
import org.junit.After;
import org.junit.Before;

/**
 * To execute selenium tests in IDE, you have to run the selenium server first :
 * mvn selenium:start-server   
 */
public abstract class SonarTestCase {
  protected DefaultSelenium selenium;

  @Before
  public void before() throws Exception {
    // TODO the browser and the url must be configurable 
    selenium = new DefaultSelenium("localhost", 4444, "*firefox", "http://localhost:9000");
    selenium.start();
  }

  @After
  public void after() {
    selenium.stop();
  }

  protected void login(String login, String password) {
    logout();
    selenium.open("/sessions/new");
    selenium.type("login", login);
    selenium.type("password", password);
    selenium.click("commit");
    selenium.waitForPageToLoad("30000");
  }

  protected void logout() {
    selenium.open("/sessions/logout");
  }

  protected void loginAsAdministrator() {
    login("admin", "admin");
  }

  protected void loginAsAnonymous() {
    logout();  
  }

  protected void clickAndWait(String url) {
    selenium.click(url);
    selenium.waitForPageToLoad("30000");
  }
}
