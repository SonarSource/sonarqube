/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import PageSidebar, { PageSidebarProps } from '../PageSidebar';

it('should render correctly', () => {
  const sidebar = shallowRender({
    query: { size: '3' },
    view: 'overall'
  });

  expect(sidebar).toMatchSnapshot();
});

it('should render correctly with no applications', () => {
  const sidebar = shallowRender({
    applicationsEnabled: false,
    query: { size: '3' },
    view: 'overall'
  });

  expect(sidebar).toMatchSnapshot();
});

it('should render `leak` view correctly', () => {
  const sidebar = shallowRender({
    query: { view: 'leak' },
    view: 'leak'
  });
  expect(sidebar).toMatchSnapshot();
});

it('should render `leak` view correctly with no applications', () => {
  const sidebar = shallowRender({
    applicationsEnabled: false,
    query: { view: 'leak' },
    view: 'leak'
  });
  expect(sidebar).toMatchSnapshot();
});

function shallowRender(overrides: Partial<PageSidebarProps> = {}) {
  return shallow(
    <PageSidebar
      applicationsEnabled={true}
      onClearAll={jest.fn()}
      onQueryChange={jest.fn()}
      query={{ view: 'overall' }}
      view="overall"
      {...overrides}
    />
  );
}
