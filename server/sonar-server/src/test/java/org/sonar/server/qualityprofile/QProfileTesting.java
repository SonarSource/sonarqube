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
package org.sonar.server.qualityprofile;

import org.sonar.db.qualityprofile.QualityProfileDto;

/**
 * Utility class for tests involving quality profiles
 */
public class QProfileTesting {

  public static final QProfileName XOO_P1_NAME = new QProfileName("xoo", "P1");
  public static final String XOO_P1_KEY = "XOO_P1";
  public static final QProfileName XOO_P2_NAME = new QProfileName("xoo", "P2");
  public static final String XOO_P2_KEY = "XOO_P2";
  public static final QProfileName XOO_P3_NAME = new QProfileName("xoo", "P3");
  public static final String XOO_P3_KEY = "XOO_P3";

  public static QualityProfileDto newQProfileDto(QProfileName name, String key) {
    return QualityProfileDto.createFor(key).setName(name.getName()).setLanguage(name.getLanguage());
  }

  public static QualityProfileDto newXooP1() {
    return newQProfileDto(XOO_P1_NAME, XOO_P1_KEY);
  }

  public static QualityProfileDto newXooP2() {
    return newQProfileDto(XOO_P2_NAME, XOO_P2_KEY);
  }

  public static QualityProfileDto newXooP3() {
    return newQProfileDto(XOO_P3_NAME, XOO_P3_KEY);
  }
}
