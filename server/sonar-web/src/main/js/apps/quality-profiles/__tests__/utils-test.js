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
import { sortProfiles } from '../utils';

function createProfile (key, parentKey) {
  return { name: key, key, parentKey };
}

function checkOrder (list, order) {
  const listKeys = list.map(item => item.key);
  expect(listKeys).toEqual(order);
}

describe('#sortProfiles', () => {
  it('should sort when no parents', () => {
    const profile1 = createProfile('profile1');
    const profile2 = createProfile('profile2');
    const profile3 = createProfile('profile3');
    checkOrder(
        sortProfiles([profile1, profile2, profile3]),
        ['profile1', 'profile2', 'profile3']
    );
  });

  it('should sort by name', () => {
    const profile1 = createProfile('profile1');
    const profile2 = createProfile('profile2');
    const profile3 = createProfile('profile3');
    checkOrder(
        sortProfiles([profile3, profile1, profile2]),
        ['profile1', 'profile2', 'profile3']
    );
  });

  it('should sort with children', () => {
    const child1 = createProfile('child1', 'parent');
    const child2 = createProfile('child2', 'parent');
    const parent = createProfile('parent');
    checkOrder(
        sortProfiles([child1, child2, parent]),
        ['parent', 'child1', 'child2']
    );
  });

  it('should sort single branch', () => {
    const profile1 = createProfile('profile1');
    const profile2 = createProfile('profile2', 'profile3');
    const profile3 = createProfile('profile3', 'profile1');
    checkOrder(
        sortProfiles([profile3, profile2, profile1]),
        ['profile1', 'profile3', 'profile2']
    );
  });
});
