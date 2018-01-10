/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { getBadgeUrl, BadgeOptions, BadgeType } from '../utils';

jest.mock('../../../../helpers/urls', () => ({
  getHostUrl: () => 'host'
}));

const options: BadgeOptions = {
  branch: 'master',
  color: 'white',
  project: 'foo',
  metric: 'alert_status'
};

describe('#getBadgeUrl', () => {
  it('should generate correct marketing badge links', () => {
    expect(getBadgeUrl(BadgeType.marketing, options)).toBe(
      'host/images/project_badges/sonarcloud-white.svg'
    );
    expect(getBadgeUrl(BadgeType.marketing, { ...options, color: 'orange' })).toBe(
      'host/images/project_badges/sonarcloud-orange.svg'
    );
  });

  it('should generate correct quality gate badge links', () => {
    expect(getBadgeUrl(BadgeType.qualityGate, options));
  });

  it('should generate correct measures badge links', () => {
    expect(getBadgeUrl(BadgeType.measure, options)).toBe(
      'host/api/project_badges/measure?branch=master&project=foo&metric=alert_status'
    );
  });

  it('should ignore undefined parameters', () => {
    expect(getBadgeUrl(BadgeType.measure, { color: 'white', metric: 'alert_status' })).toBe(
      'host/api/project_badges/measure?metric=alert_status'
    );
  });
});
