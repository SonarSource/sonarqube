/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { RawHotspot, RiskExposure } from '../../types/securityHotspots';

export function mockHotspot(overrides: Partial<RawHotspot> = {}): RawHotspot {
  return {
    key: '01fc972e-2a3c-433e-bcae-0bd7f88f5123',
    component: 'com.github.kevinsawicki:http-request:com.github.kevinsawicki.http.HttpRequest',
    project: 'com.github.kevinsawicki:http-request',
    rule: 'checkstyle:com.puppycrawl.tools.checkstyle.checks.coding.MagicNumberCheck',
    status: 'RESOLVED',
    resolution: 'FALSE-POSITIVE',
    securityCategory: 'command-injection',
    vulnerabilityProbability: RiskExposure.HIGH,
    message: "'3' is a magic number.",
    line: 81,
    author: 'Developer 1',
    creationDate: '2013-05-13T17:55:39+0200',
    updateDate: '2013-05-13T17:55:39+0200',
    ...overrides
  };
}
