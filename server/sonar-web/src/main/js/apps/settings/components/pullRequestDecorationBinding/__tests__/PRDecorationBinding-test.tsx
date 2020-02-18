/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import {
  deleteProjectAlmBinding,
  getAlmSettings,
  getProjectAlmBinding,
  setProjectAzureBinding,
  setProjectBitbucketBinding,
  setProjectGithubBinding
} from '../../../../../api/alm-settings';
import { mockComponent } from '../../../../../helpers/testMocks';
import { AlmKeys } from '../../../../../types/alm-settings';
import PRDecorationBinding from '../PRDecorationBinding';

jest.mock('../../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([]),
  getProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
  setProjectAzureBinding: jest.fn().mockResolvedValue(undefined),
  setProjectBitbucketBinding: jest.fn().mockResolvedValue(undefined),
  setProjectGithubBinding: jest.fn().mockResolvedValue(undefined),
  deleteProjectAlmBinding: jest.fn().mockResolvedValue(undefined)
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
    repository: 'account/repo'
  };
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(instances);
  (getProjectAlmBinding as jest.Mock).mockResolvedValueOnce(formdata);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().formData).toEqual(formdata);
  expect(wrapper.state().originalData).toEqual(formdata);
});

it('should handle reset', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({
    formData: {
      key: 'whatever',
      repository: 'something/else'
    }
  });

  wrapper.instance().handleReset();
  await waitAndUpdate(wrapper);

  expect(deleteProjectAlmBinding).toBeCalledWith(PROJECT_KEY);
  expect(wrapper.state().formData).toEqual({ key: '', repository: '', slug: '' });
  expect(wrapper.state().originalData).toBeUndefined();
});

describe('handleSubmit', () => {
  const instances = [
    { key: 'github', alm: AlmKeys.GitHub },
    { key: 'azure', alm: AlmKeys.Azure },
    { key: 'bitbucket', alm: AlmKeys.Bitbucket }
  ];

  it('should work for github', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const githubKey = 'github';
    const repository = 'repo/path';
    wrapper.setState({ formData: { key: githubKey, repository }, instances });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectGithubBinding).toBeCalledWith({
      almSetting: githubKey,
      project: PROJECT_KEY,
      repository
    });
    expect(wrapper.state().success).toBe(true);
  });

  it('should work for azure', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const azureKey = 'azure';
    wrapper.setState({ formData: { key: azureKey }, instances });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectAzureBinding).toBeCalledWith({
      almSetting: azureKey,
      project: PROJECT_KEY
    });
    expect(wrapper.state().success).toBe(true);
  });

  it('should work for bitbucket', async () => {
    const wrapper = shallowRender();
    await waitAndUpdate(wrapper);
    const bitbucketKey = 'bitbucket';
    const repository = 'repoKey';
    const slug = 'repoSlug';
    wrapper.setState({ formData: { key: bitbucketKey, repository, slug }, instances });
    wrapper.instance().handleSubmit();
    await waitAndUpdate(wrapper);

    expect(setProjectBitbucketBinding).toBeCalledWith({
      almSetting: bitbucketKey,
      project: PROJECT_KEY,
      repository,
      slug
    });
    expect(wrapper.state().success).toBe(true);
  });
});

it('should handle failures gracefully', async () => {
  (getProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status: 500 });
  (setProjectGithubBinding as jest.Mock).mockRejectedValueOnce({ status: 500 });
  (deleteProjectAlmBinding as jest.Mock).mockRejectedValueOnce({ status: 500 });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.setState({
    formData: {
      key: 'whatever',
      repository: 'something/else'
    }
  });

  wrapper.instance().handleSubmit();
  await waitAndUpdate(wrapper);
  wrapper.instance().handleReset();
});

it('should handle field changes', async () => {
  const url = 'git.enterprise.com';
  const repository = 'my/repo';
  const instances = [
    { key: 'instance1', url, alm: 'github' },
    { key: 'instance2', url, alm: 'github' },
    { key: 'instance3', url: 'otherurl', alm: 'github' }
  ];
  (getAlmSettings as jest.Mock).mockResolvedValueOnce(instances);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleFieldChange('key', 'instance2');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2'
  });

  wrapper.instance().handleFieldChange('repository', repository);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().formData).toEqual({
    key: 'instance2',
    repository
  });
});

it('should validate form', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.instance().validateForm({ key: '', repository: '' })).toBe(false);
  expect(wrapper.instance().validateForm({ key: '', repository: 'c' })).toBe(false);

  wrapper.setState({
    instances: [
      { key: 'azure', alm: AlmKeys.Azure },
      { key: 'bitbucket', alm: AlmKeys.Bitbucket },
      { key: 'github', alm: AlmKeys.GitHub },
      { key: 'gitlab', alm: AlmKeys.GitLab }
    ]
  });

  expect(wrapper.instance().validateForm({ key: 'azure' })).toBe(true);

  expect(wrapper.instance().validateForm({ key: 'github', repository: '' })).toBe(false);
  expect(wrapper.instance().validateForm({ key: 'github', repository: 'asdf' })).toBe(true);

  expect(wrapper.instance().validateForm({ key: 'bitbucket', repository: 'key' })).toBe(false);
  expect(
    wrapper.instance().validateForm({ key: 'bitbucket', repository: 'key', slug: 'slug' })
  ).toBe(true);

  expect(wrapper.instance().validateForm({ key: 'gitlab' })).toBe(true);
  expect(wrapper.instance().validateForm({ key: 'gitlab', repository: 'key' })).toBe(true);
});

function shallowRender(props: Partial<PRDecorationBinding['props']> = {}) {
  return shallow<PRDecorationBinding>(
    <PRDecorationBinding component={mockComponent({ key: PROJECT_KEY })} {...props} />
  );
}
