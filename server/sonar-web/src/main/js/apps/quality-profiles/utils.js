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
// @flow
import sortBy from 'lodash/sortBy';
import moment from 'moment';

type Profiles = Array<{
  key: string,
  name: string,
  parentKey?: string
}>;

export function sortProfiles (profiles: Profiles) {
  const result = [];
  const sorted = sortBy(profiles, 'name');

  function retrieveChildren (parent) {
    return sorted.filter(p => (
        (parent == null && p.parentKey == null) ||
        (parent != null && p.parentKey === parent.key)
    ));
  }

  function putProfile (profile = null, depth = 0) {
    const children = retrieveChildren(profile);

    if (profile != null) {
      result.push({ ...profile, childrenCount: children.length, depth });
    }

    children.forEach(child => putProfile(child, depth + 1));
  }

  putProfile();

  return result;
}

export function createFakeProfile (overrides: {}) {
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

export function isStagnant (profile: { userUpdatedAt: string }) {
  return moment().diff(moment(profile.userUpdatedAt), 'years') >= 1;
}
