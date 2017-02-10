/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package util.selenium;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;

class ThreadSafeDriver {
  private ThreadSafeDriver() {
    // Static class
  }

  static SeleniumDriver makeThreadSafe(final RemoteWebDriver driver) {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          driver.quit();
        } catch (UnreachableBrowserException e) {
          // Ignore. The browser was killed properly
        }
      }
    }));

    return (SeleniumDriver) Proxy.newProxyInstance(
      Thread.currentThread().getContextClassLoader(),
      findInterfaces(driver),
      new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          if (method.getName().equals("quit")) {
            return null; // We don't want anybody to quit() our (per thread) driver
          }

          try {
            return method.invoke(driver, args);
          } catch (InvocationTargetException e) {
            throw e.getCause();
          }
        }
      });
  }

  private static Class[] findInterfaces(Object driver) {
    Set<Class<?>> interfaces = new LinkedHashSet<>();

    interfaces.add(SeleniumDriver.class);

    for (Class<?> parent = driver.getClass(); parent != null; ) {
      Collections.addAll(interfaces, parent.getInterfaces());
      parent = parent.getSuperclass();
    }

    return interfaces.toArray(new Class[interfaces.size()]);
  }
}
