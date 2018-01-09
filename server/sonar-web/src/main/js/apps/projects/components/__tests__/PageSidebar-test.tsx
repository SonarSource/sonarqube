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
import PageSidebar from '../PageSidebar';

it('should render correctly', () => {
  const sidebar = shallow(
    <PageSidebar
      onClearAll={jest.fn()}
      onQueryChange={jest.fn()}
      query={{ size: '3' }}
      showFavoriteFilter={true}
      view="overall"
      visualization="risk"
    />
  );
  expect(sidebar).toMatchSnapshot();
});

it('should render `leak` view correctly', () => {
  const sidebar = shallow(
    <PageSidebar
      onClearAll={jest.fn()}
      onQueryChange={jest.fn()}
      query={{ view: 'leak' }}
      showFavoriteFilter={true}
      view="leak"
      visualization="risk"
    />
  );
  expect(sidebar).toMatchSnapshot();
});

it('reset function should work correctly with view and visualizations', () => {
  const sidebar = shallow(
    <PageSidebar
      onClearAll={jest.fn()}
      onQueryChange={jest.fn()}
      query={{ view: 'visualizations', visualization: 'bugs' }}
      showFavoriteFilter={true}
      view="visualizations"
      visualization="bugs"
    />
  );
  expect(sidebar.find('ClearAll').exists()).toBeFalsy();
  sidebar.setProps({ query: { size: '3' } });
  expect(sidebar.find('ClearAll').exists()).toBeTruthy();
});
