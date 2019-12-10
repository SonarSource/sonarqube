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
import { mockRawHotspot } from '../../../helpers/mocks/security-hotspots';
import { RiskExposure } from '../../../types/security-hotspots';
import { groupByCategory, mapRules, sortHotspots } from '../utils';

const hotspots = [
  mockRawHotspot({
    key: '3',
    vulnerabilityProbability: RiskExposure.HIGH,
    securityCategory: 'object-injection',
    message: 'tfdh'
  }),
  mockRawHotspot({
    key: '5',
    vulnerabilityProbability: RiskExposure.MEDIUM,
    securityCategory: 'xpath-injection',
    message: 'asdf'
  }),
  mockRawHotspot({
    key: '1',
    vulnerabilityProbability: RiskExposure.HIGH,
    securityCategory: 'dos',
    message: 'a'
  }),
  mockRawHotspot({
    key: '7',
    vulnerabilityProbability: RiskExposure.LOW,
    securityCategory: 'ssrf',
    message: 'rrrr'
  }),
  mockRawHotspot({
    key: '2',
    vulnerabilityProbability: RiskExposure.HIGH,
    securityCategory: 'dos',
    message: 'b'
  }),
  mockRawHotspot({
    key: '8',
    vulnerabilityProbability: RiskExposure.LOW,
    securityCategory: 'ssrf',
    message: 'sssss'
  }),
  mockRawHotspot({
    key: '4',
    vulnerabilityProbability: RiskExposure.MEDIUM,
    securityCategory: 'log-injection',
    message: 'asdf'
  }),
  mockRawHotspot({
    key: '9',
    vulnerabilityProbability: RiskExposure.LOW,
    securityCategory: 'xxe',
    message: 'aaa'
  }),
  mockRawHotspot({
    key: '6',
    vulnerabilityProbability: RiskExposure.LOW,
    securityCategory: 'xss',
    message: 'zzz'
  })
];

const categories = {
  'object-injection': {
    title: 'Object Injection'
  },
  'xpath-injection': {
    title: 'XPath Injection'
  },
  'log-injection': {
    title: 'Log Injection'
  },
  dos: {
    title: 'Denial of Service (DoS)'
  },
  ssrf: {
    title: 'Server-Side Request Forgery (SSRF)'
  },
  xxe: {
    title: 'XML External Entity (XXE)'
  },
  xss: {
    title: 'Cross-Site Scripting (XSS)'
  }
};

describe('sortHotspots', () => {
  it('should sort properly', () => {
    const result = sortHotspots(hotspots, categories);

    expect(result.map(h => h.key)).toEqual(['1', '2', '3', '4', '5', '6', '7', '8', '9']);
  });
});

describe('groupByCategory', () => {
  it('should group and sort properly', () => {
    const result = groupByCategory(hotspots, categories);

    expect(result).toHaveLength(7);
    expect(result.map(g => g.key)).toEqual([
      'xss',
      'dos',
      'log-injection',
      'object-injection',
      'ssrf',
      'xxe',
      'xpath-injection'
    ]);
  });
});

describe('mapRules', () => {
  it('should map names to keys', () => {
    const rules = [
      { key: 'a', name: 'A rule' },
      { key: 'b', name: 'B rule' },
      { key: 'c', name: 'C rule' }
    ];

    expect(mapRules(rules)).toEqual({
      a: 'A rule',
      b: 'B rule',
      c: 'C rule'
    });
  });
});
