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
package org.sonar.db.issue;

/**
 * Lightweight (branch uuid, issue key) row used to keyset-paginate Security Hotspot findings during the
 * Hotspots-to-Issues migration (MMF-5734). Paginating on a fan-out-free query keeps page sizes exact, so the
 * migration never holds a cursor open while it mutates the very rows it is reading.
 */
public class HotspotMigrationKeyDto {
  private String branchUuid = "";
  private String kee = "";

  public String getBranchUuid() {
    return branchUuid;
  }

  public HotspotMigrationKeyDto setBranchUuid(String branchUuid) {
    this.branchUuid = branchUuid;
    return this;
  }

  public String getKee() {
    return kee;
  }

  public HotspotMigrationKeyDto setKee(String kee) {
    this.kee = kee;
    return this;
  }
}
