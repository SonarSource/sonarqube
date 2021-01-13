/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getAlmSettings } from '../../../../api/alm-settings';
import { mockAppState, mockLoggedInUser } from '../../../../helpers/testMocks';
import { AlmKeys } from '../../../../types/alm-settings';
import { ProjectCreationMenu } from '../ProjectCreationMenu';

jest.mock('../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([])
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({ currentUser: mockLoggedInUser({ permissions: { global: [] } }) })
  ).toMatchSnapshot('not allowed');
});

it('should fetch alm bindings on mount', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getAlmSettings).toBeCalled();
});

it('should not fetch alm bindings if user cannot create projects', async () => {
  const wrapper = shallowRender({ currentUser: mockLoggedInUser({ permissions: { global: [] } }) });
  await waitAndUpdate(wrapper);
  expect(getAlmSettings).not.toBeCalled();
});

it('should not fetch alm bindings if branches are not enabled', async () => {
  const wrapper = shallowRender({ appState: mockAppState({ branchesEnabled: false }) });
  await waitAndUpdate(wrapper);
  expect(getAlmSettings).not.toBeCalled();
});

it('should filter alm bindings appropriately', async () => {
  (getAlmSettings as jest.Mock).mockResolvedValueOnce([
    { alm: AlmKeys.Azure },
    { alm: AlmKeys.Bitbucket, url: 'b1' },
    { alm: AlmKeys.Bitbucket, url: 'b2' },
    { alm: AlmKeys.GitHub },
    { alm: AlmKeys.GitLab, url: 'gitlab.com' }
  ]);

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(wrapper.state().boundAlms).toEqual([AlmKeys.GitHub, AlmKeys.GitLab]);
});

function shallowRender(overrides: Partial<ProjectCreationMenu['props']> = {}) {
  return shallow<ProjectCreationMenu>(
    <ProjectCreationMenu
      appState={mockAppState({ branchesEnabled: true })}
      currentUser={mockLoggedInUser({ permissions: { global: ['provisioning'] } })}
      {...overrides}
    />
  );
}
