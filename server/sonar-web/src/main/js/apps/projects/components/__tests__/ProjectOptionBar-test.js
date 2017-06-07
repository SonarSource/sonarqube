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
import ProjectsOptionBar from '../ProjectsOptionBar';
import { click } from '../../../../helpers/testUtils';

it('should render option bar closed', () => {
  expect(shallow(<ProjectsOptionBar open={false} view="overall" />)).toMatchSnapshot();
});

it('should render option bar open', () => {
  expect(
    shallow(
      <ProjectsOptionBar
        open={true}
        view="leak"
        visualization="risk"
        projects={[1, 2, 3]}
        projectsAppState={{ total: 3 }}
        user={{ isLoggedIn: true }}
      />
    )
  ).toMatchSnapshot();
});

it('should render disabled sorting options for visualizations', () => {
  expect(
    shallow(<ProjectsOptionBar open={true} view="visualizations" visualization="coverage" />)
  ).toMatchSnapshot();
});

it('should call close method correctly', () => {
  const toggle = jest.fn();
  const wrapper = shallow(<ProjectsOptionBar open={true} view="leak" onToggleOptionBar={toggle} />);
  click(wrapper.find('.projects-topbar-button'));
  expect(toggle.mock.calls).toMatchSnapshot();
});

it('should render switch the default sorting option for anonymous users', () => {
  expect(
    shallow(
      <ProjectsOptionBar
        open={true}
        view="overall"
        visualization="risk"
        user={{ isLoggedIn: true }}
      />
    ).find('ProjectsSortingSelect')
  ).toMatchSnapshot();
  expect(
    shallow(
      <ProjectsOptionBar
        open={true}
        view="leak"
        visualization="risk"
        user={{ isLoggedIn: false }}
      />
    ).find('ProjectsSortingSelect')
  ).toMatchSnapshot();
});
