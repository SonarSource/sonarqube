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
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { getBreadcrumbs } from '../../../../api/components';
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { keydown, waitAndUpdate } from '../../../../helpers/testUtils';
import Breadcrumbs from '../Breadcrumbs';

jest.mock('../../../../api/components', () => ({
  getBreadcrumbs: jest.fn().mockResolvedValue([
    { key: 'anc1', name: 'Ancestor1' },
    { key: 'anc2', name: 'Ancestor2' },
    { key: 'bar', name: 'Bar' },
  ]),
}));

const componentFoo = {
  key: 'foo',
  name: 'Foo',
  qualifier: 'TRK',
};

const componentBar = {
  key: 'bar',
  name: 'Bar',
  qualifier: 'TRK',
};

beforeEach(() => {
  (getBreadcrumbs as jest.Mock<any>).mockClear();
});

it('should display correctly for the list view', () => {
  const wrapper = mount(
    <Breadcrumbs
      backToFirst={false}
      component={componentBar}
      handleSelect={() => {}}
      rootComponent={componentFoo}
    />
  );
  expect(wrapper).toMatchSnapshot();
});

it('should display only the root component', () => {
  const wrapper = shallowRender({ component: componentFoo });
  expect(wrapper.state()).toMatchSnapshot();
});

it('should load the breadcrumb from the api', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getBreadcrumbs).toHaveBeenCalled();
});

it('should correctly handle keyboard action', async () => {
  const handleSelect = jest.fn();
  const wrapper = shallowRender({ handleSelect });
  await waitAndUpdate(wrapper);
  keydown({ key: KeyboardKeys.LeftArrow });
  expect(handleSelect).toHaveBeenCalled();
});

function shallowRender(props: Partial<Breadcrumbs['props']> = {}) {
  return shallow<Breadcrumbs>(
    <Breadcrumbs
      backToFirst={false}
      component={componentBar}
      handleSelect={() => {}}
      rootComponent={componentFoo}
      {...props}
    />
  );
}
