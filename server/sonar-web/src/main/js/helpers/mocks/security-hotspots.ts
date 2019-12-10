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
import { ComponentQualifier } from '../../types/component';
import {
  DetailedHotspot,
  DetailedHotspotRule,
  RawHotspot,
  RiskExposure
} from '../../types/security-hotspots';
import { mockComponent, mockUser } from '../testMocks';

export function mockRawHotspot(overrides: Partial<RawHotspot> = {}): RawHotspot {
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

export function mockDetailledHotspot(overrides?: Partial<DetailedHotspot>): DetailedHotspot {
  return {
    assignee: mockUser(),
    author: mockUser(),
    component: mockComponent({ qualifier: ComponentQualifier.File }),
    creationDate: '2013-05-13T17:55:41+0200',
    key: '01fc972e-2a3c-433e-bcae-0bd7f88f5123',
    line: 142,
    message: "'3' is a magic number.",
    project: mockComponent({ qualifier: ComponentQualifier.Project }),
    resolution: 'FALSE-POSITIVE',
    rule: mockDetailledHotspotRule(),
    status: 'RESOLVED',
    textRange: {
      startLine: 142,
      endLine: 142,
      startOffset: 26,
      endOffset: 83
    },
    updateDate: '2013-05-13T17:55:42+0200',
    ...overrides
  };
}

export function mockDetailledHotspotRule(
  overrides?: Partial<DetailedHotspotRule>
): DetailedHotspotRule {
  return {
    key: 'squid:S2077',
    name: 'That rule',
    fixRecommendations: '<p>This a <strong>strong</strong> message about fixing !</p>',
    riskDescription: '<p>This a <strong>strong</strong> message about risk !</p>',
    vulnerabilityDescription: '<p>This a <strong>strong</strong> message about vulnerability !</p>',
    vulnerabilityProbability: RiskExposure.HIGH,
    securityCategory: 'sql-injection',
    ...overrides
  };
}
