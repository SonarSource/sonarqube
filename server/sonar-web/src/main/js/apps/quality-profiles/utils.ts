/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { sortBy } from 'lodash';
import { Profile as BaseProfile } from '../../api/quality-profiles';
import { differenceInYears, isValidDate, parseDate } from '../../helpers/dates';
import { Profile } from './types';

export function sortProfiles(profiles: BaseProfile[]): Profile[] {
  const result: Profile[] = [];
  const sorted = sortBy(profiles, 'name');

  function retrieveChildren(parent: BaseProfile | null) {
    return sorted.filter(
      p => (parent == null && p.parentKey == null) || (parent != null && p.parentKey === parent.key)
    );
  }

  function putProfile(profile: BaseProfile | null = null, depth: number = 1) {
    const children = retrieveChildren(profile);

    if (profile != null) {
      result.push({ ...profile, childrenCount: children.length, depth });
    }

    children.forEach(child => putProfile(child, depth + 1));
  }

  sorted
    .filter(
      profile => profile.parentKey == null || sorted.find(p => p.key === profile.parentKey) == null
    )
    .forEach(profile => putProfile(profile));

  return result;
}

export function createFakeProfile(overrides?: any) {
  return {
    key: 'key',
    name: 'name',
    isDefault: false,
    isInherited: false,
    language: 'js',
    languageName: 'JavaScript',
    activeRuleCount: 10,
    activeDeprecatedRuleCount: 2,
    projectCount: 3,
    ...overrides
  };
}

export function isStagnant(profile: Profile): boolean {
  if (profile.userUpdatedAt) {
    const updateDate = parseDate(profile.userUpdatedAt);
    if (isValidDate(updateDate)) {
      return differenceInYears(new Date(), updateDate) >= 1;
    }
  }
  return false;
}
