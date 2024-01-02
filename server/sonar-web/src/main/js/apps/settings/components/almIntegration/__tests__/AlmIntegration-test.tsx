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
import {
  countBindedProjects,
  deleteConfiguration,
  getAlmDefinitions,
  validateAlmSettings,
} from '../../../../../api/alm-settings';
import { mockLocation, mockRouter } from '../../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import { AlmKeys, AlmSettingsBindingStatusType } from '../../../../../types/alm-settings';
import { AlmIntegration } from '../AlmIntegration';
import AlmIntegrationRenderer from '../AlmIntegrationRenderer';

jest.mock('../../../../../api/alm-settings', () => ({
  countBindedProjects: jest.fn().mockResolvedValue(0),
  deleteConfiguration: jest.fn().mockResolvedValue(undefined),
  getAlmDefinitions: jest
    .fn()
    .mockResolvedValue({ azure: [], bitbucket: [], bitbucketcloud: [], github: [], gitlab: [] }),
  validateAlmSettings: jest.fn().mockResolvedValue(''),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should validate existing configurations', async () => {
  (getAlmDefinitions as jest.Mock).mockResolvedValueOnce({
    [AlmKeys.Azure]: [{ key: 'a1' }],
    [AlmKeys.BitbucketServer]: [{ key: 'b1' }],
    [AlmKeys.BitbucketCloud]: [{ key: 'bc1' }],
    [AlmKeys.GitHub]: [{ key: 'gh1' }, { key: 'gh2' }],
    [AlmKeys.GitLab]: [{ key: 'gl1' }],
  });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(validateAlmSettings).toHaveBeenCalledTimes(6);
  expect(validateAlmSettings).toHaveBeenCalledWith('a1');
  expect(validateAlmSettings).toHaveBeenCalledWith('b1');
  expect(validateAlmSettings).toHaveBeenCalledWith('bc1');
  expect(validateAlmSettings).toHaveBeenCalledWith('gh1');
  expect(validateAlmSettings).toHaveBeenCalledWith('gh2');
  expect(validateAlmSettings).toHaveBeenCalledWith('gl1');
});

it('should handle alm selection', async () => {
  const router = mockRouter();
  const wrapper = shallowRender({ router });

  wrapper.setState({ currentAlmTab: AlmKeys.Azure });

  wrapper.instance().handleSelectAlm(AlmKeys.GitHub);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().currentAlmTab).toBe(AlmKeys.GitHub);
  expect(router.push).toHaveBeenCalled();
});

it('should handle delete', async () => {
  const toBeDeleted = '45672';
  (countBindedProjects as jest.Mock).mockResolvedValueOnce(7);
  const wrapper = shallowRender();

  wrapper.find(AlmIntegrationRenderer).props().onDelete(toBeDeleted);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectCount).toBe(7);
  expect(wrapper.state().definitionKeyForDeletion).toBe(toBeDeleted);

  wrapper.find(AlmIntegrationRenderer).props().onCancelDelete();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectCount).toBeUndefined();
  expect(wrapper.state().definitionKeyForDeletion).toBeUndefined();
});

it('should delete configuration', async () => {
  (deleteConfiguration as jest.Mock).mockResolvedValueOnce(undefined);
  const wrapper = shallowRender();
  wrapper.instance().handleConfirmDelete('8345678');

  await waitAndUpdate(wrapper);
  expect(wrapper.state().projectCount).toBeUndefined();
  expect(wrapper.state().definitionKeyForDeletion).toBeUndefined();
});

it('should validate a configuration', async () => {
  const definitionKey = 'validated-key';
  const failureMessage = 'an error occured';

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  (validateAlmSettings as jest.Mock)
    .mockRejectedValueOnce(undefined)
    .mockResolvedValueOnce(failureMessage)
    .mockResolvedValueOnce('')
    .mockResolvedValueOnce('');

  await wrapper.instance().handleCheck(definitionKey);

  expect(wrapper.state().definitionStatus[definitionKey]).toEqual({
    alertSuccess: true,
    failureMessage: '',
    type: AlmSettingsBindingStatusType.Warning,
  });

  await wrapper.instance().handleCheck(definitionKey);

  expect(wrapper.state().definitionStatus[definitionKey]).toEqual({
    alertSuccess: true,
    failureMessage,
    type: AlmSettingsBindingStatusType.Failure,
  });

  await wrapper.instance().handleCheck(definitionKey);

  expect(wrapper.state().definitionStatus[definitionKey]).toEqual({
    alertSuccess: true,
    failureMessage: '',
    type: AlmSettingsBindingStatusType.Success,
  });
});

it('should fetch settings', async () => {
  const definitions = {
    [AlmKeys.Azure]: [{ key: 'a1' }],
    [AlmKeys.BitbucketServer]: [{ key: 'b1' }],
    [AlmKeys.BitbucketCloud]: [{ key: 'bc1' }],
    [AlmKeys.GitHub]: [{ key: 'gh1' }],
    [AlmKeys.GitLab]: [{ key: 'gl1' }],
  };

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  (getAlmDefinitions as jest.Mock).mockResolvedValueOnce(definitions);

  await wrapper.instance().fetchPullRequestDecorationSetting();

  expect(getAlmDefinitions).toHaveBeenCalled();
  expect(wrapper.state().definitions).toEqual(definitions);
  expect(wrapper.state().loadingAlmDefinitions).toBe(false);
});

it('should detect the current ALM from the query', () => {
  let wrapper = shallowRender({ location: mockLocation() });
  expect(wrapper.state().currentAlmTab).toBe(AlmKeys.GitHub);

  wrapper = shallowRender({ location: mockLocation({ query: { alm: AlmKeys.BitbucketCloud } }) });
  expect(wrapper.state().currentAlmTab).toBe(AlmKeys.BitbucketServer);

  wrapper.setProps({ location: mockLocation({ query: { alm: AlmKeys.GitLab } }) });
  expect(wrapper.state().currentAlmTab).toBe(AlmKeys.GitLab);
});

function shallowRender(props: Partial<AlmIntegration['props']> = {}) {
  return shallow<AlmIntegration>(
    <AlmIntegration
      hasFeature={jest.fn().mockReturnValue(true)}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}
