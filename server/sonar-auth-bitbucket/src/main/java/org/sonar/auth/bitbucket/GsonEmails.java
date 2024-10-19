/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.auth.bitbucket;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import javax.annotation.CheckForNull;

public class GsonEmails {

  @SerializedName("values")
  private List<GsonEmail> emails;

  public GsonEmails() {
    // even if empty constructor is not required for Gson, it is strongly
    // recommended:
    // http://stackoverflow.com/a/18645370/229031
  }

  public List<GsonEmail> getEmails() {
    return emails;
  }

  public static GsonEmails parse(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, GsonEmails.class);
  }

  @CheckForNull
  public String extractPrimaryEmail() {
    for (GsonEmail gsonEmail : emails) {
      if (gsonEmail.isPrimary()) {
        return gsonEmail.getEmail();
      }
    }
    return null;
  }
}
