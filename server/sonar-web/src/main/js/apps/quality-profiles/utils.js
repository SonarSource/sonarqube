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
import { sortBy } from 'lodash';
import moment from 'moment';
import type { Profile } from './propTypes';

export function sortProfiles(profiles: Array<Profile>) {
  const result = [];
  const sorted = sortBy(profiles, 'name');

  function retrieveChildren(parent) {
    return sorted.filter(
      p => (parent == null && p.parentKey == null) || (parent != null && p.parentKey === parent.key)
    );
  }

  function putProfile(profile = null, depth = 1) {
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

export function createFakeProfile(overrides: {}) {
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

export function isStagnant(profile: Profile) {
  return moment().diff(moment(profile.userUpdatedAt), 'years') >= 1;
}

export const getProfilesPath = (organization: ?string) =>
  (organization ? `/organizations/${organization}/quality_profiles` : '/profiles');

export const getProfilesForLanguagePath = (language: string, organization: ?string) => ({
  pathname: getProfilesPath(organization),
  query: { language }
});

export const getProfilePath = (name: string, language: string, organization: ?string) => ({
  pathname: getProfilesPath(organization) + '/show',
  query: { name, language }
});

export const getProfileComparePath = (
  name: string,
  language: string,
  organization: ?string,
  withKey?: string
) => {
  const query: Object = { language, name };
  if (withKey) {
    Object.assign(query, { withKey });
  }
  return {
    pathname: getProfilesPath(organization) + '/compare',
    query
  };
};

export const getProfileChangelogPath = (
  name: string,
  language: string,
  organization: ?string,
  filter?: { since?: string, to?: string }
) => {
  const query: Object = { language, name };
  if (filter) {
    if (filter.since) {
      Object.assign(query, { since: filter.since });
    }
    if (filter.to) {
      Object.assign(query, { to: filter.to });
    }
  }
  return {
    pathname: getProfilesPath(organization) + '/changelog',
    query
  };
};
