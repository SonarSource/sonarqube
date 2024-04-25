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
import { HotspotRatingEnum } from 'design-system';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { Standards } from '../../types/security';
import {
  Hotspot,
  HotspotComment,
  HotspotComponent,
  HotspotResolution,
  HotspotRule,
  HotspotStatus,
  RawHotspot,
  ReviewHistoryElement,
  ReviewHistoryType,
} from '../../types/security-hotspots';
import { mockFlowLocation, mockUser } from '../testMocks';

export function mockRawHotspot(overrides: Partial<RawHotspot> = {}): RawHotspot {
  return {
    key: '01fc972e-2a3c-433e-bcae-0bd7f88f5123',
    component: 'com.github.kevinsawicki:http-request:com.github.kevinsawicki.http.HttpRequest',
    project: 'com.github.kevinsawicki:http-request',
    rule: 'checkstyle:com.puppycrawl.tools.checkstyle.checks.coding.MagicNumberCheck',
    status: HotspotStatus.TO_REVIEW,
    resolution: undefined,
    securityCategory: 'command-injection',
    vulnerabilityProbability: HotspotRatingEnum.HIGH,
    message: "'3' is a magic number.",
    line: 81,
    author: 'Developer 1',
    creationDate: '2013-05-13T17:55:39+0200',
    updateDate: '2013-05-13T17:55:39+0200',
    ...overrides,
  };
}

export function mockHotspot(overrides?: Partial<Hotspot>): Hotspot {
  const assigneeUser = mockUser({ login: 'assignee' });
  const authorUser = mockUser({ login: 'author' });
  return {
    assignee: 'assignee',
    assigneeUser,
    author: 'author',
    authorUser,
    canChangeStatus: true,
    changelog: [],
    comment: [],
    component: mockHotspotComponent({ qualifier: ComponentQualifier.File }),
    creationDate: '2013-05-13T17:55:41+0200',
    flows: [{ locations: [mockFlowLocation()] }],
    key: '01fc972e-2a3c-433e-bcae-0bd7f88f5123',
    line: 6,
    message: "'3' is a magic number.",
    project: mockHotspotComponent({ qualifier: ComponentQualifier.Project }),
    resolution: HotspotResolution.FIXED,
    rule: mockHotspotRule(),
    status: HotspotStatus.REVIEWED,
    textRange: {
      startLine: 6,
      endLine: 6,
      startOffset: 3,
      endOffset: 9,
    },
    updateDate: '2013-05-13T17:55:42+0200',
    users: [assigneeUser, authorUser],
    ...overrides,
  };
}

export function mockHotspotComponent(overrides?: Partial<HotspotComponent>): HotspotComponent {
  return {
    key: 'hotspot-component',
    name: 'Hotspot Component',
    longName: 'Hotspot component long name',
    qualifier: ComponentQualifier.File,
    path: 'path/to/component',
    ...overrides,
  };
}

export function mockHotspotComment(overrides?: Partial<HotspotComment>): HotspotComment {
  return {
    key: 'comment-1',
    createdAt: '2018-09-10',
    htmlText: '<strong>TEST</strong>',
    markdown: '*TEST*',
    updatable: false,
    login: 'dude-2',
    user: mockUser({ login: 'dude-2' }),
    ...overrides,
  };
}

export function mockHotspotRule(overrides?: Partial<HotspotRule>): HotspotRule {
  return {
    key: 'squid:S2077',
    name: 'That rule',
    vulnerabilityProbability: HotspotRatingEnum.HIGH,
    securityCategory: 'sql-injection',
    ...overrides,
  };
}

export function mockHotspotReviewHistoryElement(
  overrides?: Partial<ReviewHistoryElement>,
): ReviewHistoryElement {
  return {
    date: '2019-09-13T17:55:42+0200',
    type: ReviewHistoryType.Creation,
    user: mockUser(),
    ...overrides,
  };
}

export function mockStandards(): Standards {
  return {
    cwe: {
      unknown: {
        title: 'No CWE associated',
      },
      '1004': {
        title: "Sensitive Cookie Without 'HttpOnly' Flag",
      },
    },
    owaspTop10: {
      a1: {
        title: 'Injection',
      },
      a2: {
        title: 'Broken Authentication',
      },
      a3: {
        title: 'Sensitive Data Exposure',
      },
    },
    'owaspTop10-2021': {
      a1: {
        title: 'Injection',
      },
      a2: {
        title: 'Broken Authentication',
      },
      a3: {
        title: 'Sensitive Data Exposure',
      },
    },
    sonarsourceSecurity: {
      'buffer-overflow': {
        title: 'Buffer Overflow',
      },
      'sql-injection': {
        title: 'SQL Injection',
      },
      rce: {
        title: 'Code Injection (RCE)',
      },
    },
    'pciDss-3.2': {
      '1': {
        title: ' Install and maintain a firewall configuration to protect cardholder data',
      },
    },
    'pciDss-4.0': {
      '2': {
        title: 'This is useless...',
      },
    },
    'owaspAsvs-4.0': {
      '1': {
        title: 'New OWASP ASVS cat 1',
      },
    },
  };
}
