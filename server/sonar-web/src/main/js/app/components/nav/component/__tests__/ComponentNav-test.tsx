/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

jest.mock('../ComponentNavFavorite', () => ({
  default: function ComponentNavFavorite() {
    return null;
  }
}));

jest.mock('../ComponentNavBreadcrumbs', () => ({
  default: function ComponentNavBreadcrumbs() {
    return null;
  }
}));

jest.mock('../ComponentNavMenu', () => ({
  default: function ComponentNavMenu() {
    return null;
  }
}));

jest.mock('../../../RecentHistory', () => ({
  default: { add: jest.fn() }
}));

jest.mock('../../../../../api/ce', () => ({
  getTasksForComponent: jest.fn(() => Promise.resolve({ queue: [] }))
}));

import * as React from 'react';
import { mount, shallow } from 'enzyme';
import ComponentNav from '../ComponentNav';

const getTasksForComponent = require('../../../../../api/ce').getTasksForComponent as jest.Mock<
  any
>;

const component = {
  breadcrumbs: [{ key: 'component', name: 'component', qualifier: 'TRK' }],
  key: 'component',
  name: 'component',
  organization: 'org',
  qualifier: 'TRK'
};

it('loads status', () => {
  getTasksForComponent.mockClear();
  mount(<ComponentNav branches={[]} component={component} conf={{}} location={{}} />);
  expect(getTasksForComponent).toBeCalledWith('component');
});

it('renders', () => {
  const wrapper = shallow(
    <ComponentNav branches={[]} component={component} conf={{}} location={{}} />
  );
  wrapper.setState({
    incremental: true,
    isFailed: true,
    isInProgress: true,
    isPending: true
  });
  expect(wrapper).toMatchSnapshot();
});
