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
  createAzureConfiguration,
  createBitbucketCloudConfiguration,
  createBitbucketServerConfiguration,
  createGithubConfiguration,
  createGitlabConfiguration,
  deleteConfiguration,
  updateAzureConfiguration,
  updateBitbucketCloudConfiguration,
  updateBitbucketServerConfiguration,
  updateGithubConfiguration,
  updateGitlabConfiguration,
  validateAlmSettings,
} from '../../../../../api/alm-settings';
import {
  mockAzureBindingDefinition,
  mockBitbucketCloudBindingDefinition,
  mockBitbucketServerBindingDefinition,
  mockGithubBindingDefinition,
  mockGitlabBindingDefinition,
} from '../../../../../helpers/mocks/alm-settings';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import { AlmBindingDefinition, AlmKeys } from '../../../../../types/alm-settings';
import AlmBindingDefinitionForm from '../AlmBindingDefinitionForm';
import AlmBindingDefinitionFormRenderer from '../AlmBindingDefinitionFormRenderer';

jest.mock('../../../../../api/alm-settings', () => ({
  createAzureConfiguration: jest.fn().mockResolvedValue({}),
  createBitbucketCloudConfiguration: jest.fn().mockResolvedValue({}),
  createBitbucketServerConfiguration: jest.fn().mockResolvedValue({}),
  createGithubConfiguration: jest.fn().mockResolvedValue({}),
  createGitlabConfiguration: jest.fn().mockResolvedValue({}),
  updateAzureConfiguration: jest.fn().mockResolvedValue({}),
  updateBitbucketCloudConfiguration: jest.fn().mockResolvedValue({}),
  updateBitbucketServerConfiguration: jest.fn().mockResolvedValue({}),
  updateGithubConfiguration: jest.fn().mockResolvedValue({}),
  updateGitlabConfiguration: jest.fn().mockResolvedValue({}),
  validateAlmSettings: jest.fn().mockResolvedValue(undefined),
  deleteConfiguration: jest.fn().mockResolvedValue(undefined),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender({ bindingDefinition: undefined })).toMatchSnapshot('create');
  expect(shallowRender({ bindingDefinition: mockGithubBindingDefinition() })).toMatchSnapshot(
    'edit'
  );
});

it('should handle field changes', () => {
  const formData = mockGithubBindingDefinition();

  const wrapper = shallowRender();

  wrapper.instance().handleFieldChange('key', formData.key);
  wrapper.instance().handleFieldChange('url', formData.url);
  wrapper.instance().handleFieldChange('appId', formData.appId);
  wrapper.instance().handleFieldChange('clientId', formData.clientId);
  wrapper.instance().handleFieldChange('clientSecret', formData.clientSecret);
  wrapper.instance().handleFieldChange('privateKey', formData.privateKey);
  expect(wrapper.state().formData).toEqual(formData);
  expect(wrapper.state().touched).toBe(true);
});

it('should handle form submit', async () => {
  const afterSubmit = jest.fn();
  const formData = mockGithubBindingDefinition();

  const wrapper = shallowRender({ afterSubmit });

  wrapper.instance().setState({ formData });
  await wrapper.instance().handleFormSubmit();
  expect(afterSubmit).toHaveBeenCalledWith(formData);
});

it('should handle validation error during submit, and cancellation', async () => {
  const afterSubmit = jest.fn();
  const formData = mockGithubBindingDefinition();
  const error = 'This a test error message';
  (validateAlmSettings as jest.Mock).mockResolvedValueOnce(error);

  const wrapper = shallowRender({ afterSubmit, enforceValidation: true });

  wrapper.instance().setState({ formData });
  await wrapper.instance().handleFormSubmit();
  expect(validateAlmSettings).toHaveBeenCalledWith(formData.key);
  expect(wrapper.state().validationError).toBe(error);
  expect(afterSubmit).not.toHaveBeenCalledWith();

  wrapper.find(AlmBindingDefinitionFormRenderer).props().onCancel();
  expect(deleteConfiguration).toHaveBeenCalledWith(formData.key);
});

it.each([
  [AlmKeys.Azure, undefined, createAzureConfiguration],
  [AlmKeys.Azure, mockAzureBindingDefinition(), updateAzureConfiguration],
  [AlmKeys.GitLab, undefined, createGitlabConfiguration],
  [AlmKeys.GitLab, mockGitlabBindingDefinition(), updateGitlabConfiguration],
  [AlmKeys.GitHub, undefined, createGithubConfiguration],
  [AlmKeys.GitHub, mockGithubBindingDefinition(), updateGithubConfiguration],
  [AlmKeys.BitbucketServer, undefined, createBitbucketServerConfiguration],
  [
    AlmKeys.BitbucketServer,
    mockBitbucketServerBindingDefinition(),
    updateBitbucketServerConfiguration,
  ],
])(
  'should call the proper api on submit for %s | %s',
  async (
    alm: AlmKeys,
    bindingDefinition: AlmBindingDefinition | undefined,
    api: (def: AlmBindingDefinition) => any
  ) => {
    const wrapper = shallowRender({ alm, bindingDefinition });

    await waitAndUpdate(wrapper);
    await wrapper.instance().handleFormSubmit();
    expect(api).toHaveBeenCalled();
  }
);

it('should call the proper api for BBC', async () => {
  const wrapper = shallowRender({
    // Reminder: due to the way the settings app works, we never pass AlmKeys.BitbucketCloud as `alm`.
    alm: AlmKeys.BitbucketServer,
    bindingDefinition: undefined,
  });

  wrapper.instance().handleBitbucketVariantChange(AlmKeys.BitbucketCloud);

  await waitAndUpdate(wrapper);
  await wrapper.instance().handleFormSubmit();
  expect(createBitbucketCloudConfiguration).toHaveBeenCalled();

  wrapper.setProps({ bindingDefinition: mockBitbucketCloudBindingDefinition() });
  await wrapper.instance().handleFormSubmit();
  expect(updateBitbucketCloudConfiguration).toHaveBeenCalled();
});

it('should store bitbucket variant', async () => {
  const wrapper = shallowRender();

  wrapper
    .find(AlmBindingDefinitionFormRenderer)
    .props()
    .onBitbucketVariantChange(AlmKeys.BitbucketCloud);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().bitbucketVariant).toBe(AlmKeys.BitbucketCloud);
  expect(wrapper.state().formData).toEqual({
    clientId: '',
    clientSecret: '',
    key: '',
    workspace: '',
  });
});

it('should (dis)allow submit by validating its state (Bitbucket Cloud)', () => {
  const wrapper = shallowRender({
    // Reminder: due to the way the settings app works, we never pass AlmKeys.BitbucketCloud as `alm`.
    alm: AlmKeys.BitbucketServer,
    bindingDefinition: mockBitbucketCloudBindingDefinition(),
  });
  expect(wrapper.instance().canSubmit()).toBe(false);

  wrapper.setState({
    formData: mockBitbucketCloudBindingDefinition({ workspace: 'foo/bar' }),
    touched: true,
  });
  expect(wrapper.instance().canSubmit()).toBe(false);

  wrapper.setState({ formData: mockBitbucketCloudBindingDefinition() });
  expect(wrapper.instance().canSubmit()).toBe(true);
});

it('should (dis)allow submit by validating its state (others)', () => {
  const wrapper = shallowRender();
  expect(wrapper.instance().canSubmit()).toBe(false);

  wrapper.setState({ formData: mockGithubBindingDefinition(), touched: true });
  expect(wrapper.instance().canSubmit()).toBe(true);
});

function shallowRender(props: Partial<AlmBindingDefinitionForm['props']> = {}) {
  return shallow<AlmBindingDefinitionForm>(
    <AlmBindingDefinitionForm
      alm={AlmKeys.GitHub}
      bindingDefinition={mockGithubBindingDefinition()}
      onCancel={jest.fn()}
      afterSubmit={jest.fn()}
      {...props}
    />
  );
}
