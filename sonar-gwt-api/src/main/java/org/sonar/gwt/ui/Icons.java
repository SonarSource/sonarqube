/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.gwt.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * All icons are 16x16 pixels
 */
public final class Icons {
  private static IconBundle INSTANCE;

  private Icons() {
    // only static methods
  }

  public static IconBundle get() {
    if (INSTANCE == null) {
      INSTANCE = GWT.create(IconBundle.class);
    }
    return INSTANCE;
  }

  public static AbstractImagePrototype forQualifier(final String qualifier) {
    AbstractImagePrototype image;
    if ("FIL".equals(qualifier)) {
      image = get().qualifierFile();
    } else if ("CLA".equals(qualifier)) {
      image = get().qualifierClass();

    } else if ("PAC".equals(qualifier)) {
      image = get().qualifierPackage();

    } else if ("DIR".equals(qualifier)) {
      image = get().qualifierDirectory();

    } else if ("BRC".equals(qualifier)) {
      image = get().qualifierModule();

    } else if ("TRK".equals(qualifier)) {
      image = get().qualifierProject();

    } else if ("UTS".equals(qualifier)) {
      image = get().qualifierUnitTest();

    } else if ("FLD".equals(qualifier)) {
      image = get().qualifierField();

    } else if ("MET".equals(qualifier)) {
      image = get().qualifierMethod();

    } else if ("LIB".equals(qualifier)) {
      image = get().qualifierLibrary();

    } else {
      image = get().empty();
    }
    return image;
  }

  /**
   * @since 2.2
   * @deprecated since 2.5 use {@link Icons#forSeverity(String)}
   */
  public static AbstractImagePrototype forPriority(final String priority) {
    return forSeverity(priority);
  }

  /**
   * @since 2.5
   */
  public static AbstractImagePrototype forSeverity(final String severity) {
    AbstractImagePrototype image;
    if ("BLOCKER".equals(severity)) {
      image = get().priorityBlocker();

    } else if ("CRITICAL".equals(severity)) {
      image = get().priorityCritical();

    } else if ("MAJOR".equals(severity)) {
      image = get().priorityMajor();

    } else if ("MINOR".equals(severity)) {
      image = get().priorityMinor();

    } else if ("INFO".equals(severity)) {
      image = get().priorityInfo();

    } else {
      image = get().empty();
    }
    return image;
  }

  public static interface IconBundle extends ImageBundle {
    AbstractImagePrototype empty();

    AbstractImagePrototype zoom();

    AbstractImagePrototype information();

    AbstractImagePrototype help();

    AbstractImagePrototype qualifierField();

    AbstractImagePrototype qualifierMethod();

    AbstractImagePrototype qualifierClass();

    AbstractImagePrototype qualifierFile();

    AbstractImagePrototype qualifierUnitTest();

    AbstractImagePrototype qualifierDirectory();

    AbstractImagePrototype qualifierPackage();

    AbstractImagePrototype qualifierProject();

    AbstractImagePrototype qualifierModule();

    AbstractImagePrototype qualifierLibrary();

    AbstractImagePrototype statusOk();

    AbstractImagePrototype statusError();

    AbstractImagePrototype statusWarning();

    AbstractImagePrototype priorityBlocker();

    AbstractImagePrototype priorityCritical();

    AbstractImagePrototype priorityMajor();

    AbstractImagePrototype priorityMinor();

    AbstractImagePrototype priorityInfo();
  }
}