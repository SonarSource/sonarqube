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
package org.sonar.api.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Depending on its scope, a widget can be available for project dashboards <code>(value = "PROJECT")</code>,
 * global dashboards <code>(value = "GLOBAL")</code> or both <code>(value = {"PROJECT", "GLOBAL"})</code>.
 * 
 * <p>Before version 3.1 all widget had a scope <code>"PROJECT"</code>. If a widget is not annotated with @WidgetScope,
 * then is is assumed project scoped.
 *
 * @since 3.1
/**
 * @deprecated since 6.2, this extension is ignored as dashboards have been removed
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WidgetScope {
  String PROJECT = "PROJECT";
  String GLOBAL = "GLOBAL";

  String[] value() default PROJECT;
}
