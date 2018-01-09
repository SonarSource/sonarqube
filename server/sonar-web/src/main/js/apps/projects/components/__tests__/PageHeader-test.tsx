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
import PageHeader from '../PageHeader';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly while loading', () => {
  expect(shallowRender({ loading: true, total: 2 })).toMatchSnapshot();
});

it('should not render projects total', () => {
  expect(
    shallowRender({ total: undefined })
      .find('#projects-total')
      .exists()
  ).toBeFalsy();
});

it('should render disabled sorting options for visualizations', () => {
  expect(
    shallowRender({
      open: true,
      total: undefined,
      view: 'visualizations',
      visualization: 'coverage'
    })
  ).toMatchSnapshot();
});

it('should render switch the default sorting option for anonymous users', () => {
  expect(
    shallowRender({
      currentUser: { isLoggedIn: true },
      open: true,
      visualization: 'risk'
    }).find('ProjectsSortingSelect')
  ).toMatchSnapshot();

  expect(
    shallowRender({
      currentUser: { isLoggedIn: false },
      open: true,
      view: 'leak',
      visualization: 'risk'
    }).find('ProjectsSortingSelect')
  ).toMatchSnapshot();
});

function shallowRender(props?: {}) {
  return shallow(
    <PageHeader
      currentUser={{ isLoggedIn: false }}
      isFavorite={false}
      loading={false}
      onPerspectiveChange={jest.fn()}
      onQueryChange={jest.fn()}
      onSonarCloud={false}
      onSortChange={jest.fn()}
      projects={[]}
      query={{ search: 'test' }}
      selectedSort="size"
      total={12}
      view="overall"
      {...props}
    />
  );
}
