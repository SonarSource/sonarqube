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
package org.sonar.plugins.core.timemachine;

import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.i18n.I18n;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Periods implements BatchExtension {

  private final Snapshot snapshot;
  private final I18n i18n;

  public Periods(Snapshot snapshot, I18n i18n) {
    this.snapshot = snapshot;
    this.i18n = i18n;
  }

  public String getLabel(int periodIndex) {
    String mode = snapshot.getPeriodMode(periodIndex);
    String param = snapshot.getPeriodModeParameter(periodIndex);
    Date date = snapshot.getPeriodDate(periodIndex);

    if (mode.equals(CoreProperties.TIMEMACHINE_MODE_DAYS)) {
      return message("over_x_days", param);
    } else if (mode.equals(CoreProperties.TIMEMACHINE_MODE_VERSION)) {
      if (date != null) {
        return message("since_version_detailed", param, convertDate(date));
      } else {
        return message("since_version", param);
      }
    } else if (mode.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS)) {
      if (date != null) {
        return message("since_previous_analysis_detailed", convertDate(date));
      } else {
        return message("since_previous_analysis");
      }
    } else if (mode.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION)) {
      if (param != null) {
        return message("since_previous_version_detailed", param);
      } else {
        return message("since_previous_version");
      }
    } else if (mode.equals(CoreProperties.TIMEMACHINE_MODE_DATE)) {
      return message("since_x", convertDate(date));
    } else {
      throw new IllegalStateException("This mode is not supported : " + mode);
    }
  }

  private String message(String key, Object... parameters) {
    return i18n.message(getLocale(), key, null, parameters);
  }

  private String convertDate(Date date){
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MMM dd");
    return dateFormat.format(date);
  }

  private Locale getLocale() {
    return Locale.ENGLISH;
  }

}
