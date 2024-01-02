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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockComponent } from '../../../../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser, mockMetric } from '../../../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../../../types/component';
import ProjectBadges from '../badges/ProjectBadges';
import { ProjectInformation } from '../ProjectInformation';
import { ProjectInformationPages } from '../ProjectInformationPages';

jest.mock('../../../../../../api/measures', () => {
  const { mockMeasure } = jest.requireActual('../../../../../../helpers/testMocks');
  return {
    getMeasures: jest.fn().mockResolvedValue([mockMeasure()]),
  };
});

it('should render correctly', async () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ currentUser: mockLoggedInUser() })).toMatchSnapshot('logged in user');
  expect(shallowRender({ component: mockComponent({ visibility: 'private' }) })).toMatchSnapshot(
    'private'
  );
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('measures loaded');
});

it('should handle page change', async () => {
  const wrapper = shallowRender();

  wrapper.instance().setPage(ProjectInformationPages.badges);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().page).toBe(ProjectInformationPages.badges);
});

it('should display badge', () => {
  const wrapper = shallowRender({
    component: mockComponent({ qualifier: ComponentQualifier.Project }),
  });

  expect(wrapper.find(ProjectBadges).type).toBeDefined();

  wrapper.setProps({ component: mockComponent({ qualifier: ComponentQualifier.Application }) });
  expect(wrapper.find(ProjectBadges).type).toBeDefined();
});

function shallowRender(props: Partial<ProjectInformation['props']> = {}) {
  return shallow<ProjectInformation>(
    <ProjectInformation
      component={mockComponent()}
      currentUser={mockCurrentUser()}
      metrics={{
        coverage: mockMetric(),
      }}
      onComponentChange={jest.fn()}
      {...props}
    />
  );
}
