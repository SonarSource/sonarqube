/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import reducer, {
  getOrganizationByKey,
  areThereCustomOrganizations,
  getMyOrganizations,
  State,
  receiveOrganizations,
  receiveMyOrganizations,
  createOrganization,
  updateOrganization,
  deleteOrganization
} from '../organizations';

const state0: State = { byKey: {}, my: [] };

describe('Reducer', () => {
  it('should have initial state', () => {
    // @ts-ignore `undefined` is passed when the redux store is created,
    // however should not be allowed by typings
    expect(reducer(undefined, {})).toMatchSnapshot();
  });

  it('should receive organizations', () => {
    const state1 = reducer(
      state0,
      receiveOrganizations([{ key: 'foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }])
    );
    expect(state1).toMatchSnapshot();

    const state2 = reducer(state1, receiveOrganizations([{ key: 'foo', name: 'Qwe' }]));
    expect(state2).toMatchSnapshot();
  });

  it('should receive my organizations', () => {
    const state1 = reducer(
      state0,
      receiveMyOrganizations([{ key: 'foo', name: 'Foo' }, { key: 'bar', name: 'Bar' }])
    );
    expect(state1).toMatchSnapshot();
  });

  it('should create organization', () => {
    const state1 = reducer(state0, createOrganization({ key: 'foo', name: 'foo' }));
    expect(state1).toMatchSnapshot();
  });

  it('should update organization', () => {
    const state1 = reducer(state0, createOrganization({ key: 'foo', name: 'foo' }));
    const state2 = reducer(
      state1,
      updateOrganization('foo', { name: 'bar', description: 'description' })
    );
    expect(state2).toMatchSnapshot();
  });

  it('should delete organization', () => {
    const state1 = reducer(state0, createOrganization({ key: 'foo', name: 'foo' }));
    const state2 = reducer(state1, deleteOrganization('foo'));
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

  it('getMyOrganizations', () => {
    expect(getMyOrganizations(state0)).toEqual([]);

    const foo = { key: 'foo', name: 'Foo' };
    expect(getMyOrganizations({ ...state0, byKey: { foo }, my: ['foo'] })).toEqual([foo]);
  });

  it('areThereCustomOrganizations', () => {
    const foo = { key: 'foo', name: 'Foo' };
    const bar = { key: 'bar', name: 'Bar' };
    expect(areThereCustomOrganizations({ ...state0, byKey: {} })).toBe(false);
    expect(areThereCustomOrganizations({ ...state0, byKey: { foo } })).toBe(false);
    expect(areThereCustomOrganizations({ ...state0, byKey: { foo, bar } })).toBe(true);
  });
});
