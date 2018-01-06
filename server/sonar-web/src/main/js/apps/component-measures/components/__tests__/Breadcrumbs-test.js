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
import React from 'react';
import { mount } from 'enzyme';
import Breadcrumbs from '../Breadcrumbs';
import { doAsync } from '../../../../helpers/testUtils';

jest.mock('../../../../api/components', () => ({
  getBreadcrumbs: () =>
    Promise.resolve([
      { key: 'anc1', name: 'Ancestor1' },
      { key: 'anc2', name: 'Ancestor2' },
      { key: 'bar', name: 'Bar' }
    ])
}));

it('should display correctly for the list view', () => {
  const wrapper = mount(
    <Breadcrumbs
      branch={{ isMain: true }}
      component={{ key: 'bar', name: 'Bar' }}
      handleSelect={() => {}}
      rootComponent={{ key: 'foo', name: 'Foo' }}
      view="list"
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should display only the root component', () => {
  const wrapper = mount(
    <Breadcrumbs
      branch={{ isMain: true }}
      component={{ key: 'foo', name: 'Foo' }}
      handleSelect={() => {}}
      rootComponent={{ key: 'foo', name: 'Foo' }}
      view="tree"
    />
  );
  expect(wrapper.state()).toMatchSnapshot();
});

it.only('should load the breadcrumb from the api', () => {
  const wrapper = mount(
    <Breadcrumbs
      branch={{ isMain: true }}
      component={{ key: 'bar', name: 'Bar' }}
      handleSelect={() => {}}
      rootComponent={{ key: 'foo', name: 'Foo' }}
      view="tree"
    />
  );
  return doAsync(() => {
    expect(wrapper.state()).toMatchSnapshot();
  });
});
