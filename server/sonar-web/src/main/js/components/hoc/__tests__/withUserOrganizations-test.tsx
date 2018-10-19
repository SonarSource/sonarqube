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
import * as React from 'react';
import { shallow } from 'enzyme';
import { createStore } from 'redux';
import { Organization } from '../../../app/types';
import { withUserOrganizations } from '../withUserOrganizations';

jest.mock('../../../api/organizations', () => ({ getOrganizations: jest.fn() }));

class X extends React.Component<{ userOrganizations: Organization[] }> {
  render() {
    return <div />;
  }
}

const UnderTest = withUserOrganizations(X);

// TODO Find a way to make this work, currently getting the following error : Actions must be plain objects. Use custom middleware for async actions.
it.skip('should pass user organizations and logged in user', () => {
  const org = { key: 'my-org', name: 'My Organization' };
  const store = createStore(state => state, {
    organizations: { byKey: { 'my-org': org }, my: ['my-org'] }
  });
  const wrapper = shallow(<UnderTest />, { context: { store } });
  const wrappedComponent = wrapper
    .dive()
    .dive()
    .dive();
  expect(wrappedComponent.type()).toBe(X);
  expect(wrappedComponent.prop('userOrganizations')).toEqual([org]);
});
