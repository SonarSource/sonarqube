/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.process;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SecurityManagement {
  private static final String CACHE_TTL_KEY = "networkaddress.cache.ttl";

  private SecurityManagement() {
    // static only
  }

  public static void restrictPlugins() {
    SecurityManager sm = new SecurityManager();
    Policy.setPolicy(new CustomPolicy());
    System.setSecurityManager(sm);
    // SONAR-14870 By default, with a security manager installed, the DNS cache never times out. See InetAddressCachePolicy.
    if (Security.getProperty(CACHE_TTL_KEY) == null) {
      Security.setProperty(CACHE_TTL_KEY, "30");
    }
  }

  static class CustomPolicy extends Policy {
    private static final Set<String> BLOCKED_RUNTIME_PERMISSIONS = new HashSet<>(Arrays.asList(
      "createClassLoader",
      "getClassLoader",
      "setContextClassLoader",
      "enableContextClassLoaderOverride",
      "closeClassLoader",
      "setSecurityManager",
      "createSecurityManager"
    ));
    private static final Set<String> BLOCKED_SECURITY_PERMISSIONS = new HashSet<>(Arrays.asList(
      "createAccessControlContext",
      "setPolicy"
    ));

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
      // classloader used to load plugins
      String clName = getDomainClassLoaderName(domain);
      if ("org.sonar.classloader.ClassRealm".equals(clName)) {
        if (permission instanceof RuntimePermission && BLOCKED_RUNTIME_PERMISSIONS.contains(permission.getName())) {
          return false;
        }
        if (permission instanceof SecurityPermission && BLOCKED_SECURITY_PERMISSIONS.contains(permission.getName())) {
          return false;
        }
      }
      return true;
    }

    // workaround for SONAR-13559 / JDK-8014008
    // borrowed as-is from https://github.com/elastic/elasticsearch/pull/14274
    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
      // code should not rely on this method, or at least use it correctly:
      // https://bugs.openjdk.java.net/browse/JDK-8014008
      // return them a new empty permissions object so jvisualvm etc work
      for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
        if ("sun.rmi.server.LoaderHandler".equals(element.getClassName()) &&
          "loadClass".equals(element.getMethodName())) {
          return new Permissions();
        }
      }
      // return UNSUPPORTED_EMPTY_COLLECTION since it is safe.
      return super.getPermissions(codesource);
    }

    String getDomainClassLoaderName(ProtectionDomain domain) {
      ClassLoader classLoader = domain.getClassLoader();
      return classLoader != null ? classLoader.getClass().getName() : null;
    }
  }
}
