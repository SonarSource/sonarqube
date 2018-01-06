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
package com.sonarsource;

import com.sonarsource.decimal_scale_of_measures.DecimalScaleMeasureComputer;
import com.sonarsource.decimal_scale_of_measures.DecimalScaleMetric;
import com.sonarsource.decimal_scale_of_measures.DecimalScaleProperty;
import com.sonarsource.decimal_scale_of_measures.DecimalScaleSensor;
import org.sonar.api.Plugin;

import static java.util.Arrays.asList;

public class BatchPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(asList(
      // SONAR-6939 decimal_scale_of_measures
      DecimalScaleMeasureComputer.class,
      DecimalScaleMetric.class,
      DecimalScaleSensor.class,
      DecimalScaleProperty.definition(),

      DumpSettingsInitializer.class,
      RaiseMessageException.class,
      TempFolderExtension.class,
      WaitingSensor.class
      ));
  }
}
