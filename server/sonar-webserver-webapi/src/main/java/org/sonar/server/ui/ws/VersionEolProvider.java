/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.ui.ws;

import org.sonar.api.utils.System2;
import org.sonar.server.platform.ws.LicenseSupportTypeReader;
import org.sonar.server.platform.ws.SupportType;

import static org.sonar.api.internal.MetadataLoader.loadSqIsLta;
import static org.sonar.api.internal.MetadataLoader.loadSqPremiumVersionEol;
import static org.sonar.api.internal.MetadataLoader.loadSqVersionEol;

public class VersionEolProvider {

  private final LicenseSupportTypeReader licenseSupportTypeReader;

  public VersionEolProvider(LicenseSupportTypeReader licenseSupportTypeReader) {
    this.licenseSupportTypeReader = licenseSupportTypeReader;
  }

  public String getEffectiveVersionEol() {
    if (loadSqIsLta(System2.INSTANCE) && SupportType.PREMIUM.equals(licenseSupportTypeReader.getSupportType())) {
      return loadSqPremiumVersionEol(System2.INSTANCE);
    }
    return loadSqVersionEol(System2.INSTANCE);
  }
}
