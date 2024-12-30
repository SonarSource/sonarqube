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

import { Location } from '~sonar-aligned/types/router';
import { MetricKey } from '../../../../sonar-aligned/types/metrics';
import { BadgeOptions, BadgeType, getBadgeSnippet, getBadgeUrl } from '../utils';

jest.mock('../../../../helpers/urls', () => ({
  ...jest.requireActual('../../../../helpers/urls'),
  getHostUrl: () => 'host',
  getPathUrlAsString: (o: Location) => `host${o.pathname}${o.search}`,
}));

const options: BadgeOptions = {
  branch: 'master',
  metric: 'alert_status',
  project: 'foo',
};

describe('#getBadgeUrl', () => {
  it('should generate correct quality gate badge links', () => {
    expect(getBadgeUrl(BadgeType.qualityGate, options, 'foo')).toBe(
      'host/api/project_badges/quality_gate?branch=master&project=foo&token=foo',
    );
  });

  it('should generate correct measures badge links', () => {
    expect(getBadgeUrl(BadgeType.measure, options, 'foo')).toBe(
      'host/api/project_badges/measure?branch=master&project=foo&metric=alert_status&token=foo',
    );
  });

  it('should generate correct ai code assurance badge links', () => {
    expect(getBadgeUrl(BadgeType.aiCodeAssurance, options, 'foo')).toBe(
      'host/api/project_badges/ai_code_assurance?branch=master&project=foo&token=foo',
    );
  });

  it('should generate correct ai code assurance badge links with timestamp', () => {
    expect(getBadgeUrl(BadgeType.aiCodeAssurance, options, 'foo', true)).toContain(
      'host/api/project_badges/ai_code_assurance?branch=master&project=foo&token=foo',
    );
  });

  it('should ignore undefined parameters', () => {
    expect(getBadgeUrl(BadgeType.measure, { metric: MetricKey.alert_status }, 'foo')).toBe(
      'host/api/project_badges/measure?metric=alert_status&token=foo',
    );
  });

  it('should force metric parameters', () => {
    expect(getBadgeUrl(BadgeType.measure, {}, 'foo')).toBe(
      'host/api/project_badges/measure?metric=alert_status&token=foo',
    );
  });
});

describe('#getBadgeSnippet', () => {
  it('should generate a correct markdown image for measure', () => {
    const snippet = getBadgeSnippet(BadgeType.measure, options, 'foo');
    expect(snippet).toBe(
      '[![alert_status](host/api/project_badges/measure?branch=master&project=foo&metric=alert_status&token=foo)](host/dashboard?id=foo&branch=master)',
    );
  });

  it('should generate a correct markdown image for ai code assurance', () => {
    const snippet = getBadgeSnippet(BadgeType.aiCodeAssurance, options, 'foo');
    expect(snippet).toBe(
      '[![overview.badges.ai_code_assurance](host/api/project_badges/ai_code_assurance?branch=master&project=foo&token=foo)](host/dashboard?id=foo&branch=master)',
    );
  });
});
