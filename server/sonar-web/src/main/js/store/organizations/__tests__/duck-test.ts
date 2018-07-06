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
import organizations, { getOrganizationByKey, areThereCustomOrganizations } from '../duck';

const state0 = { byKey: {}, my: [], groups: {} };

describe('Reducer', () => {
  it('should have initial state', () => {
    expect((organizations as any)(undefined, {})).toMatchSnapshot();
  });

  it('should receive organizations', () => {
    const action1 = {
      type: 'RECEIVE_ORGANIZATIONS',
      organizations: [{ key: 'foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }]
    };
    const state1 = organizations(state0, action1);
    expect(state1).toMatchSnapshot();

    const action2 = {
      type: 'RECEIVE_ORGANIZATIONS',
      organizations: [{ key: 'foo', name: 'Qwe' }]
    };
    const state2 = organizations(state1, action2);
    expect(state2).toMatchSnapshot();
  });
});

describe('Selectors', () => {
  it('getOrganizationByKey', () => {
    const foo = { key: 'foo', name: 'Foo' };
    const state = { ...state0, byKey: { foo } };
    expect(getOrganizationByKey(state, 'foo')).toBe(foo);
    expect(getOrganizationByKey(state, 'bar')).toBeFalsy();
  });

  it('areThereCustomOrganizations', () => {
    const foo = { key: 'foo', name: 'Foo' };
    const bar = { key: 'bar', name: 'Bar' };
    expect(areThereCustomOrganizations({ ...state0, byKey: {} })).toBe(false);
    expect(areThereCustomOrganizations({ ...state0, byKey: { foo } })).toBe(false);
    expect(areThereCustomOrganizations({ ...state0, byKey: { foo, bar } })).toBe(true);
  });
});
