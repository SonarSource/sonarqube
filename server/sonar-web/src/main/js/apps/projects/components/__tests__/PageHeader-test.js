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
import React from 'react';
import { shallow } from 'enzyme';
import PageHeader from '../PageHeader';

it('should render correctly', () => {
  expect(
    shallow(<PageHeader query={{ search: 'test' }} projectsAppState={{ total: 12 }} />)
  ).toMatchSnapshot();
});

it('should render correctly while loading', () => {
  expect(
    shallow(
      <PageHeader
        query={{ search: '' }}
        isFavorite={true}
        projectsAppState={{ loading: true, total: 2 }}
      />
    )
  ).toMatchSnapshot();
});

it('should not render projects total', () => {
  expect(
    shallow(<PageHeader projectsAppState={{}} query={{ search: '' }} />)
      .find('#projects-total')
      .exists()
  ).toBeFalsy();
});

it('should render disabled sorting options for visualizations', () => {
  expect(
    shallow(
      <PageHeader
        open={true}
        projectsAppState={{}}
        view="visualizations"
        visualization="coverage"
      />
    )
  ).toMatchSnapshot();
});

it('should render switch the default sorting option for anonymous users', () => {
  expect(
    shallow(
      <PageHeader
        currentUser={{ isLoggedIn: true }}
        open={true}
        projectsAppState={{}}
        view="overall"
        visualization="risk"
      />
    ).find('ProjectsSortingSelect')
  ).toMatchSnapshot();
  expect(
    shallow(
      <PageHeader
        currentUser={{ isLoggedIn: false }}
        open={true}
        projectsAppState={{}}
        view="leak"
        visualization="risk"
      />
    ).find('ProjectsSortingSelect')
  ).toMatchSnapshot();
});
