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
import { getAlmSettings } from '../../../../api/alm-settings';
import {
  mockAppState,
  mockLocation,
  mockLoggedInUser,
  mockRouter
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import AlmBindingDefinitionForm from '../../../settings/components/almIntegration/AlmBindingDefinitionForm';
import CreateProjectModeSelection from '../CreateProjectModeSelection';
import { CreateProjectPage } from '../CreateProjectPage';
import { CreateProjectModes } from '../types';

jest.mock('../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([{ alm: AlmKeys.BitbucketServer, key: 'foo' }])
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(getAlmSettings).toBeCalled();
});

it.each([
  [CreateProjectModes.Manual],
  [CreateProjectModes.AzureDevOps],
  [CreateProjectModes.BitbucketServer],
  [CreateProjectModes.BitbucketCloud],
  [CreateProjectModes.GitHub],
  [CreateProjectModes.GitLab]
])('should render correctly for %s mode', (mode: CreateProjectModes) => {
  expect(
    shallowRender({
      location: mockLocation({ query: { mode } })
    })
  ).toMatchSnapshot();
});

it('should render alm configuration creation correctly', () => {
  const wrapper = shallowRender();

  wrapper
    .find(CreateProjectModeSelection)
    .props()
    .onConfigMode(AlmKeys.Azure);
  expect(wrapper).toMatchSnapshot();
});

it('should cancel alm configuration creation properly', () => {
  const wrapper = shallowRender();

  wrapper
    .find(CreateProjectModeSelection)
    .props()
    .onConfigMode(AlmKeys.Azure);
  expect(wrapper.state().creatingAlmDefinition).toBe(AlmKeys.Azure);

  wrapper
    .find(AlmBindingDefinitionForm)
    .props()
    .onCancel();
  expect(wrapper.state().creatingAlmDefinition).toBeUndefined();
});

it('should submit alm configuration creation properly', async () => {
  const push = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push }) });

  wrapper
    .find(CreateProjectModeSelection)
    .props()
    .onConfigMode(AlmKeys.Azure);
  expect(wrapper.state().creatingAlmDefinition).toBe(AlmKeys.Azure);

  wrapper
    .find(AlmBindingDefinitionForm)
    .props()
    .afterSubmit({ key: 'test-key' });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().creatingAlmDefinition).toBeUndefined();
  expect(getAlmSettings).toHaveBeenCalled();
  expect(push).toHaveBeenCalledWith({ pathname: '/path', query: { mode: AlmKeys.Azure } });
});

it('should submit alm configuration creation properly for BBC', async () => {
  const push = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ push }) });

  wrapper
    .find(CreateProjectModeSelection)
    .props()
    .onConfigMode(AlmKeys.BitbucketServer);
  expect(wrapper.state().creatingAlmDefinition).toBe(AlmKeys.BitbucketServer);

  (getAlmSettings as jest.Mock).mockResolvedValueOnce([{ alm: AlmKeys.BitbucketCloud }]);
  wrapper
    .find(AlmBindingDefinitionForm)
    .props()
    .afterSubmit({ key: 'test-key' });
  await waitAndUpdate(wrapper);
  expect(wrapper.state().creatingAlmDefinition).toBeUndefined();
  expect(getAlmSettings).toHaveBeenCalled();
  expect(push).toHaveBeenCalledWith({ pathname: '/path', query: { mode: AlmKeys.BitbucketCloud } });
});

function shallowRender(props: Partial<CreateProjectPage['props']> = {}) {
  return shallow<CreateProjectPage>(
    <CreateProjectPage
      appState={mockAppState()}
      currentUser={mockLoggedInUser()}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}
