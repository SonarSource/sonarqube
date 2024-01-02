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
import { Location } from '../../../../../../../components/hoc/withRouter';
import { BadgeOptions, BadgeType, getBadgeSnippet, getBadgeUrl } from '../utils';

jest.mock('../../../../../../../helpers/urls', () => ({
  ...jest.requireActual('../../../../../../../helpers/urls'),
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
      'host/api/project_badges/quality_gate?branch=master&project=foo&token=foo'
    );
  });

  it('should generate correct measures badge links', () => {
    expect(getBadgeUrl(BadgeType.measure, options, 'foo')).toBe(
      'host/api/project_badges/measure?branch=master&project=foo&metric=alert_status&token=foo'
    );
  });

  it('should ignore undefined parameters', () => {
    expect(getBadgeUrl(BadgeType.measure, { metric: 'alert_status' }, 'foo')).toBe(
      'host/api/project_badges/measure?metric=alert_status&token=foo'
    );
  });

  it('should force metric parameters', () => {
    expect(getBadgeUrl(BadgeType.measure, {}, 'foo')).toBe(
      'host/api/project_badges/measure?metric=alert_status&token=foo'
    );
  });
});

describe('#getBadgeSnippet', () => {
  it('should generate a correct markdown image', () => {
    expect(getBadgeSnippet(BadgeType.measure, { ...options, format: 'md' }, 'foo')).toBe(
      '[![alert_status](host/api/project_badges/measure?branch=master&project=foo&metric=alert_status&token=foo)](host/dashboard?id=foo&branch=master)'
    );
  });
});
