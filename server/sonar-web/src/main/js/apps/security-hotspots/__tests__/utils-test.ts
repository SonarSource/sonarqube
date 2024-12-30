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

import { HotspotRatingEnum } from '~design-system';
import { mockHotspot, mockRawHotspot } from '../../../helpers/mocks/security-hotspots';
import { mockUser } from '../../../helpers/testMocks';
import {
  HotspotResolution,
  HotspotStatus,
  HotspotStatusFilter,
  HotspotStatusOption,
  RawHotspot,
  ReviewHistoryType,
} from '../../../types/security-hotspots';
import { FlowLocation, IssueChangelog } from '../../../types/types';
import {
  getHotspotReviewHistory,
  getLocations,
  getStatusAndResolutionFromStatusOption,
  getStatusFilterFromStatusOption,
  getStatusOptionFromStatusAndResolution,
  groupByCategory,
  mapRules,
  sortHotspots,
} from '../utils';

const hotspots = [
  mockRawHotspot({
    key: '3',
    vulnerabilityProbability: HotspotRatingEnum.HIGH,
    securityCategory: 'object-injection',
    message: 'tfdh',
  }),
  mockRawHotspot({
    key: '5',
    vulnerabilityProbability: HotspotRatingEnum.MEDIUM,
    securityCategory: 'xpath-injection',
    message: 'asdf',
  }),
  mockRawHotspot({
    key: '1',
    vulnerabilityProbability: HotspotRatingEnum.HIGH,
    securityCategory: 'dos',
    message: 'a',
  }),
  mockRawHotspot({
    key: '7',
    vulnerabilityProbability: HotspotRatingEnum.LOW,
    securityCategory: 'ssrf',
    message: 'rrrr',
  }),
  mockRawHotspot({
    key: '2',
    vulnerabilityProbability: HotspotRatingEnum.HIGH,
    securityCategory: 'dos',
    message: 'b',
  }),
  mockRawHotspot({
    key: '8',
    vulnerabilityProbability: HotspotRatingEnum.LOW,
    securityCategory: 'ssrf',
    message: 'sssss',
  }),
  mockRawHotspot({
    key: '4',
    vulnerabilityProbability: HotspotRatingEnum.MEDIUM,
    securityCategory: 'log-injection',
    message: 'asdf',
  }),
  mockRawHotspot({
    key: '9',
    vulnerabilityProbability: HotspotRatingEnum.LOW,
    securityCategory: 'xxe',
    message: 'aaa',
  }),
  mockRawHotspot({
    key: '6',
    vulnerabilityProbability: HotspotRatingEnum.LOW,
    securityCategory: 'xss',
    message: 'zzz',
  }),
];

const categories = {
  'object-injection': {
    title: 'Object Injection',
  },
  'xpath-injection': {
    title: 'XPath Injection',
  },
  'log-injection': {
    title: 'Log Injection',
  },
  dos: {
    title: 'Denial of Service (DoS)',
  },
  ssrf: {
    title: 'Server-Side Request Forgery (SSRF)',
  },
  xxe: {
    title: 'XML External Entity (XXE)',
  },
  xss: {
    title: 'Cross-Site Scripting (XSS)',
  },
};

describe('sortHotspots', () => {
  it('should sort properly', () => {
    const result = sortHotspots(hotspots, categories);

    expect(result.map((h) => h.key)).toEqual(['1', '2', '3', '4', '5', '6', '7', '8', '9']);
  });
});

describe('groupByCategory', () => {
  it('should group properly', () => {
    const result = groupByCategory(hotspots, categories);

    expect(result).toHaveLength(7);
  });
});

describe('mapRules', () => {
  it('should map names to keys', () => {
    const rules = [
      { key: 'a', name: 'A rule' },
      { key: 'b', name: 'B rule' },
      { key: 'c', name: 'C rule' },
    ];

    expect(mapRules(rules)).toEqual({
      a: 'A rule',
      b: 'B rule',
      c: 'C rule',
    });
  });
});

describe('getHotspotReviewHistory', () => {
  it('should properly create the review history', () => {
    const changelogElement: IssueChangelog = {
      creationDate: '2018-10-01',
      isUserActive: true,
      user: 'me',
      userName: 'me-name',
      diffs: [
        {
          key: 'assign',
          newValue: 'me',
          oldValue: 'him',
        },
      ],
    };
    const commentElement = {
      key: 'comment-1',
      createdAt: '2018-09-10',
      htmlText: '<strong>TEST</strong>',
      markdown: '*TEST*',
      updatable: true,
      login: 'dude-1',
      user: mockUser({ login: 'dude-1' }),
    };
    const commentElement1 = {
      key: 'comment-2',
      createdAt: '2018-09-11',
      htmlText: '<strong>TEST</strong>',
      markdown: '*TEST*',
      updatable: true,
      login: 'dude-2',
      user: mockUser({ login: 'dude-2' }),
    };
    const hotspot = mockHotspot({
      creationDate: '2018-09-01',
      changelog: [changelogElement],
      comment: [commentElement, commentElement1],
    });
    const reviewHistory = getHotspotReviewHistory(hotspot);

    expect(reviewHistory.length).toBe(4);
    expect(reviewHistory[3]).toEqual(
      expect.objectContaining({
        type: ReviewHistoryType.Creation,
        date: hotspot.creationDate,
        user: hotspot.authorUser,
      }),
    );
    expect(reviewHistory[2]).toEqual(
      expect.objectContaining({
        type: ReviewHistoryType.Comment,
        date: commentElement.createdAt,
        user: commentElement.user,
        html: commentElement.htmlText,
      }),
    );
    expect(reviewHistory[1]).toEqual(
      expect.objectContaining({
        type: ReviewHistoryType.Comment,
        date: commentElement1.createdAt,
        user: commentElement1.user,
        html: commentElement1.htmlText,
      }),
    );
    expect(reviewHistory[0]).toEqual(
      expect.objectContaining({
        type: ReviewHistoryType.Diff,
        date: changelogElement.creationDate,
        user: {
          avatar: changelogElement.avatar,
          name: changelogElement.userName,
          active: changelogElement.isUserActive,
        },
        diffs: changelogElement.diffs,
      }),
    );
  });
});

describe('getStatusOptionFromStatusAndResolution', () => {
  it('should return the correct values', () => {
    expect(
      getStatusOptionFromStatusAndResolution(HotspotStatus.REVIEWED, HotspotResolution.FIXED),
    ).toBe(HotspotStatusOption.FIXED);
    expect(
      getStatusOptionFromStatusAndResolution(HotspotStatus.REVIEWED, HotspotResolution.SAFE),
    ).toBe(HotspotStatusOption.SAFE);
    expect(getStatusOptionFromStatusAndResolution(HotspotStatus.REVIEWED)).toBe(
      HotspotStatusOption.FIXED,
    );
    expect(getStatusOptionFromStatusAndResolution(HotspotStatus.TO_REVIEW)).toBe(
      HotspotStatusOption.TO_REVIEW,
    );
  });
});

describe('getStatusAndResolutionFromStatusOption', () => {
  it('should return the correct values', () => {
    expect(getStatusAndResolutionFromStatusOption(HotspotStatusOption.TO_REVIEW)).toEqual({
      status: HotspotStatus.TO_REVIEW,
      resolution: undefined,
    });
    expect(getStatusAndResolutionFromStatusOption(HotspotStatusOption.FIXED)).toEqual({
      status: HotspotStatus.REVIEWED,
      resolution: HotspotResolution.FIXED,
    });
    expect(getStatusAndResolutionFromStatusOption(HotspotStatusOption.SAFE)).toEqual({
      status: HotspotStatus.REVIEWED,
      resolution: HotspotResolution.SAFE,
    });
  });
});

describe('getStatusFilterFromStatusOption', () => {
  it('should return the correct values', () => {
    expect(getStatusFilterFromStatusOption(HotspotStatusOption.TO_REVIEW)).toEqual(
      HotspotStatusFilter.TO_REVIEW,
    );
    expect(getStatusFilterFromStatusOption(HotspotStatusOption.SAFE)).toEqual(
      HotspotStatusFilter.SAFE,
    );
    expect(getStatusFilterFromStatusOption(HotspotStatusOption.FIXED)).toEqual(
      HotspotStatusFilter.FIXED,
    );
  });
});

describe('getLocations', () => {
  it('should return the correct value', () => {
    const location1: FlowLocation = {
      component: 'foo',
      msg: 'Do not use foo',
      textRange: { startLine: 7, endLine: 7, startOffset: 5, endOffset: 8 },
    };

    const location2: FlowLocation = {
      component: 'foo2',
      msg: 'Do not use foo2',
      textRange: { startLine: 7, endLine: 7, startOffset: 5, endOffset: 8 },
    };

    let rawFlows: RawHotspot['flows'] = [
      {
        locations: [location1],
      },
    ];
    expect(getLocations(rawFlows, undefined)).toEqual([location1]);

    rawFlows = [
      {
        locations: [location1, location2],
      },
    ];
    expect(getLocations(rawFlows, undefined)).toEqual([location2, location1]);

    rawFlows = [
      {
        locations: [location1, location2],
      },
      {
        locations: [],
      },
    ];
    expect(getLocations(rawFlows, 0)).toEqual([location2, location1]);
  });
});
