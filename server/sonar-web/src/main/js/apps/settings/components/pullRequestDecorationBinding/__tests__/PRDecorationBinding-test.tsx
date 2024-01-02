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
  deleteProjectAlmBinding,
  getAlmSettings,
  getProjectAlmBinding,
  setProjectAzureBinding,
  setProjectBitbucketBinding,
  setProjectBitbucketCloudBinding,
  setProjectGithubBinding,
  setProjectGitlabBinding,
  validateProjectAlmBinding,
} from '../../../../../api/alm-settings';
import {
  mockAlmSettingsInstance,
  mockProjectAlmBindingConfigurationErrors,
  mockProjectAlmBindingResponse,
} from '../../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockCurrentUser } from '../../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import { AlmKeys, AlmSettingsInstance } from '../../../../../types/alm-settings';
import { PRDecorationBinding } from '../PRDecorationBinding';
import PRDecorationBindingRenderer from '../PRDecorationBindingRenderer';

jest.mock('../../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([]),
  getProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
  setProjectAzureBinding: jest.fn().mockResolvedValue(undefined),
  setProjectBitbucketBinding: jest.fn().mockResolvedValue(undefined),
  setProjectGithubBinding: jest.fn().mockResolvedValue(undefined),
  setProjectGitlabBinding: jest.fn().mockResolvedValue(undefined),
  setProjectBitbucketCloudBinding: jest.fn().mockResolvedValue(undefined),
  deleteProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
  validateProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
}));

const PROJECT_KEY = 'project-key';

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should fill selects and fill formdata', async () => {
  const url = 'github.com';
  const instances = [{ key: 'instance1', url, alm: AlmKeys.GitHub }];
  const formdata = {
    key: 'instance1',
    repository: 'account/repo',
  };
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(instances);
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(formdata);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().formData).toEqual(formdata);
  expect(wrapper.state().isChanged).toBe(false);
});

it('should handle reset', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({
    formData: {
      key: 'whatever',
      repository: 'something/else',
      monorepo: false,
    },
  });

  wrapper.instance().handleReset();
  await waitAndUpdate(wrapper);

  expect(deleteProjectAlmBinding).toHaveBeenCalledWith(PROJECT_KEY);
  expect(wrapper.state().formData).toEqual({ key: '', repository: '', slug: '', monorepo: false });
  expect(wrapper.state().isChanged).toBe(false);
});

describe('handleSubmit', () => {
  const instances: AlmSettingsInstance[] = [
    { key: 'github', alm: AlmKeys.GitHub },
    { key: 'azure', alm: AlmKeys.Azure },
    { key: 'bitbucket', alm: AlmKeys.BitbucketServer },
    { key: 'gitlab', alm: AlmKeys.GitLab },
    { key: 'bitbucketcloud', alm: AlmKeys.BitbucketCloud },
  ];

  it('should work for github', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const githubKey = 'github';
    const repository = 'repo/path';
    const summaryCommentEnabled = true;
    const monorepo = true;
    wrapper.setState({
      formData: { key: githubKey, repository, summaryCommentEnabled, monorepo },
      instances,
    });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectGithubBinding).toHaveBeenCalledWith({
      almSetting: githubKey,
      project: PROJECT_KEY,
      repository,
      summaryCommentEnabled,
      monorepo,
    });
    expect(wrapper.state().successfullyUpdated).toBe(true);
  });

  it('should work for azure', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const azureKey = 'azure';
    const repository = 'az-rep';
    const slug = 'az-project';
    const monorepo = true;
    wrapper.setState({
      formData: { key: azureKey, repository, slug, monorepo },
      instances,
    });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectAzureBinding).toHaveBeenCalledWith({
      almSetting: azureKey,
      project: PROJECT_KEY,
      projectName: slug,
      repositoryName: repository,
      monorepo,
    });
    expect(wrapper.state().successfullyUpdated).toBe(true);
  });

  it('should work for bitbucket', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const bitbucketKey = 'bitbucket';
    const repository = 'repoKey';
    const slug = 'repoSlug';
    const monorepo = true;
    wrapper.setState({ formData: { key: bitbucketKey, repository, slug, monorepo }, instances });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectBitbucketBinding).toHaveBeenCalledWith({
      almSetting: bitbucketKey,
      project: PROJECT_KEY,
      repository,
      slug,
      monorepo,
    });
    expect(wrapper.state().successfullyUpdated).toBe(true);
  });

  it('should work for gitlab', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const gitlabKey = 'gitlab';
    const repository = 'repo';
    const monorepo = true;
    wrapper.setState({
      formData: { key: gitlabKey, repository, monorepo },
      instances,
    });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectGitlabBinding).toHaveBeenCalledWith({
      almSetting: gitlabKey,
      project: PROJECT_KEY,
      repository,
      monorepo,
    });
    expect(wrapper.state().successfullyUpdated).toBe(true);
  });

  it('should work for bitbucket cloud', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const bitbucketKey = 'bitbucketcloud';
    const repository = 'repoKey';
    const monorepo = true;
    wrapper.setState({ formData: { key: bitbucketKey, repository, monorepo }, instances: [] });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);
    expect(setProjectBitbucketCloudBinding).not.toHaveBeenCalled();

    wrapper.setState({ formData: { key: bitbucketKey, repository, monorepo }, instances });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectBitbucketCloudBinding).toHaveBeenCalledWith({
      almSetting: bitbucketKey,
      project: PROJECT_KEY,
      repository,
      monorepo,
    });
    expect(wrapper.state().successfullyUpdated).toBe(true);
  });
});

describe.each([[500], [404]])('For status %i', (status) => {
  it('should handle failures gracefully', async () => {
    const newFormData = {
      key: 'whatever',
      repository: 'something/else',
      monorepo: false,
    };

    (getProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status });
    (setProjectGithubBinding as jest.Mock).mockRejectedValueOnce({ status });
    (deleteProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status });

    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    wrapper.setState({
      formData: newFormData,
      originalData: undefined,
    });

    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);
    expect(wrapper.instance().state.originalData).toBeUndefined();
    wrapper.instance().handleReset();
    await waitAndUpdate(wrapper);
    expect(wrapper.instance().state.formData).toEqual(newFormData);
  });
});

it('should handle field changes', async () => {
  const url = 'git.enterprise.com';
  const repository = 'my/repo';
  const instances: AlmSettingsInstance[] = [
    { key: 'instance1', url, alm: AlmKeys.GitHub },
    { key: 'instance2', url, alm: AlmKeys.GitHub },
    { key: 'instance3', url: 'otherurl', alm: AlmKeys.GitHub },
  ];
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(instances);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleFieldChange('key', 'instance2');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2',
    monorepo: false,
  });

  wrapper.instance().handleFieldChange('repository', repository);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2',
    repository,
    monorepo: false,
  });

  wrapper.instance().handleFieldChange('summaryCommentEnabled', true);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2',
    monorepo: false,
    repository,
    summaryCommentEnabled: true,
  });

  wrapper.instance().handleFieldChange('monorepo', true);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2',
    repository,
    summaryCommentEnabled: true,
    monorepo: true,
  });
});

it.each([
  [AlmKeys.Azure, { monorepo: false }],
  [AlmKeys.Azure, { slug: 'test', monorepo: false }],
  [AlmKeys.Azure, { repository: 'test', monorepo: false }],
  [AlmKeys.BitbucketServer, { monorepo: false }],
  [AlmKeys.BitbucketServer, { slug: 'test', monorepo: false }],
  [AlmKeys.BitbucketServer, { repository: 'test', monorepo: false }],
  [AlmKeys.BitbucketCloud, { monorepo: false }],
  [AlmKeys.GitHub, { monorepo: false }],
  [AlmKeys.GitLab, { monorepo: false }],
])(
  'should properly reject promise for %s & %s',
  async (almKey: AlmKeys, params: { monorepo: boolean }) => {
    const wrapper = shallowRender();

    expect.assertions(1);
    await expect(
      wrapper.instance().submitProjectAlmBinding(almKey, 'binding', params)
    ).rejects.toBeUndefined();
  }
);

it('should validate form', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  const validateMethod = wrapper.instance().validateForm;

  expect(validateMethod({ key: '', repository: '', monorepo: false })).toBe(false);
  expect(validateMethod({ key: '', repository: 'c', monorepo: false })).toBe(false);

  wrapper.setState({
    instances: [
      { key: 'azure', alm: AlmKeys.Azure },
      { key: 'bitbucket', alm: AlmKeys.BitbucketServer },
      { key: 'bitbucketcloud', alm: AlmKeys.BitbucketCloud },
      { key: 'github', alm: AlmKeys.GitHub },
      { key: 'gitlab', alm: AlmKeys.GitLab },
    ],
  });

  [
    { values: { key: 'azure', monorepo: false, repository: 'rep' }, result: false },
    { values: { key: 'azure', monorepo: false, slug: 'project' }, result: false },
    {
      values: { key: 'azure', monorepo: false, repository: 'repo', slug: 'project' },
      result: true,
    },
    { values: { key: 'github', monorepo: false, repository: '' }, result: false },
    { values: { key: 'github', monorepo: false, repository: 'asdf' }, result: true },
    { values: { key: 'bitbucket', monorepo: false, repository: 'key' }, result: false },
    {
      values: { key: 'bitbucket', monorepo: false, repository: 'key', slug: 'slug' },
      result: true,
    },
    { values: { key: 'bitbucketcloud', monorepo: false, repository: '' }, result: false },
    { values: { key: 'bitbucketcloud', monorepo: false, repository: 'key' }, result: true },
    { values: { key: 'gitlab', monorepo: false }, result: false },
    { values: { key: 'gitlab', monorepo: false, repository: 'key' }, result: true },
  ].forEach(({ values, result }) => {
    expect(validateMethod(values)).toBe(result);
  });
});

it('should call the validation WS and store errors', async () => {
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(mockAlmSettingsInstance());
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(
    mockProjectAlmBindingResponse({ key: 'key' })
  );

  const errors = mockProjectAlmBindingConfigurationErrors();
  (validateProjectAlmBinding as jest.Mock).mockRejectedValueOnce(errors);

  const wrapper = shallowRender();

  wrapper.find(PRDecorationBindingRenderer).props().onCheckConfiguration();

  await waitAndUpdate(wrapper);

  expect(validateProjectAlmBinding).toHaveBeenCalledWith(PROJECT_KEY);
  expect(wrapper.state().configurationErrors).toBe(errors);
});

it('should call the validation WS after loading', async () => {
  (getAlmSettings as jest.Mock).mockResolvedValueOnce([mockAlmSettingsInstance()]);
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(
    mockProjectAlmBindingResponse({ key: 'key ' })
  );

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);

  expect(validateProjectAlmBinding).toHaveBeenCalled();
});

it('should call the validation WS upon saving', async () => {
  (getAlmSettings as jest.Mock).mockResolvedValueOnce([mockAlmSettingsInstance()]);
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(
    mockProjectAlmBindingResponse({ key: 'key ' })
  );

  const wrapper = shallowRender();

  wrapper.instance().handleFieldChange('key', 'key');
  wrapper.instance().handleSubmit();

  await waitAndUpdate(wrapper);

  expect(validateProjectAlmBinding).toHaveBeenCalled();
});

function shallowRender(props: Partial<PRDecorationBinding['props']> = {}) {
  return shallow<PRDecorationBinding>(
    <PRDecorationBinding
      currentUser={mockCurrentUser()}
      component={mockComponent({ key: PROJECT_KEY })}
      {...props}
    />
  );
}
