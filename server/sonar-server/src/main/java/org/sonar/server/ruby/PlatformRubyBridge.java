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
package org.sonar.server.ruby;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.jruby.Ruby;
import org.jruby.RubyNil;
import org.jruby.embed.InvokeFailedException;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;

public class PlatformRubyBridge implements RubyBridge {
  private static final String CALL_UPGRADE_AND_START_RB_FILENAME = "call_databaseversion_upgrade.rb";
  private static final String CALL_LOAD_JAVA_WEB_SERVICES_RB_FILENAME = "call_load_java_web_services.rb";
  private static final String CALL_INVALIDATE_METRIC_CACHE_RB_FILENAME = "call_invalidate_metric_cache.rb";

  private final RackBridge rackBridge;

  public PlatformRubyBridge(RackBridge rackBridge) {
    this.rackBridge = rackBridge;
  }

  @Override
  public RubyDatabaseMigration databaseMigration() {
    CallDatabaseVersionUpgrade callDatabaseVersionUpgrade = parseMethodScriptToInterface(
      CALL_UPGRADE_AND_START_RB_FILENAME, CallDatabaseVersionUpgrade.class
      );

    return callDatabaseVersionUpgrade::callUpgrade;
  }

  @Override
  public RubyRailsRoutes railsRoutes() {
    CallLoadJavaWebServices callLoadJavaWebServices = parseMethodScriptToInterface(
      CALL_LOAD_JAVA_WEB_SERVICES_RB_FILENAME, CallLoadJavaWebServices.class
      );

    return callLoadJavaWebServices::callLoadJavaWebServices;
  }

  @Override
  public RubyMetricCache metricCache() {
    CallInvalidateMetricCache callInvalidateMetricCache = parseMethodScriptToInterface(
      CALL_INVALIDATE_METRIC_CACHE_RB_FILENAME, CallInvalidateMetricCache.class
      );

    return callInvalidateMetricCache::callInvalidate;
  }

  /**
   * Parses a Ruby script that defines a single method and returns an instance of the specified interface type as a
   * wrapper to this Ruby method.
   */
  private <T> T parseMethodScriptToInterface(String fileName, Class<T> clazz) {
    try (InputStream in = getClass().getResourceAsStream(fileName)) {
      Ruby rubyRuntime = rackBridge.getRubyRuntime();
      JavaEmbedUtils.EvalUnit evalUnit = JavaEmbedUtils.newRuntimeAdapter().parse(rubyRuntime, in, fileName, 0);
      IRubyObject rubyObject = evalUnit.run();
      Object receiver = JavaEmbedUtils.rubyToJava(rubyObject);
      T wrapper = getInstance(rubyRuntime, receiver, clazz);
      return wrapper;
    } catch (IOException e) {
      throw new RuntimeException("Failed to load script " + fileName, e);
    }
  }

  /**
   * Fork of method {@link org.jruby.embed.internal.EmbedRubyInterfaceAdapterImpl#getInstance(Object, Class)}
   */
  @SuppressWarnings("unchecked")
  public <T> T getInstance(Ruby runtime, @Nullable Object receiver, @Nullable Class<T> clazz) {
    if (clazz == null || !clazz.isInterface()) {
      return null;
    }
    Object o;
    if (receiver == null || receiver instanceof RubyNil) {
      o = JavaEmbedUtils.rubyToJava(runtime, runtime.getTopSelf(), clazz);
    } else if (receiver instanceof IRubyObject) {
      o = JavaEmbedUtils.rubyToJava(runtime, (IRubyObject) receiver, clazz);
    } else {
      IRubyObject rubyReceiver = JavaUtil.convertJavaToRuby(runtime, receiver);
      o = JavaEmbedUtils.rubyToJava(runtime, rubyReceiver, clazz);
    }
    String name = clazz.getName();
    try {
      Class<T> c = (Class<T>) Class.forName(name, true, o.getClass().getClassLoader());
      return c.cast(o);
    } catch (ClassNotFoundException e) {
      throw new InvokeFailedException(e);
    }
  }

}
