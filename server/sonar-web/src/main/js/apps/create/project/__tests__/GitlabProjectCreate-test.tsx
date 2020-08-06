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
  checkPersonalAccessTokenIsValid,
  getGitlabProjects,
  importGitlabProject,
  setAlmPersonalAccessToken
} from '../../../../api/alm-integrations';
import { mockGitlabProject } from '../../../../helpers/mocks/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { AlmKeys } from '../../../../types/alm-settings';
import GitlabProjectCreate from '../GitlabProjectCreate';

jest.mock('../../../../api/alm-integrations', () => ({
  checkPersonalAccessTokenIsValid: jest.fn().mockResolvedValue(true),
  setAlmPersonalAccessToken: jest.fn().mockResolvedValue(null),
  getGitlabProjects: jest.fn().mockRejectedValue('error'),
  importGitlabProject: jest.fn().mockRejectedValue('error')
}));

beforeEach(jest.clearAllMocks);

const almSettingKey = 'gitlab-setting';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly check PAT on mount', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalledWith(almSettingKey);
});

it('should correctly check PAT when settings are added after mount', async () => {
  const wrapper = shallowRender({ settings: [] });
  await waitAndUpdate(wrapper);

  wrapper.setProps({
    settings: [mockAlmSettingsInstance({ alm: AlmKeys.GitLab, key: 'otherKey' })]
  });

  expect(checkPersonalAccessTokenIsValid).toBeCalledWith('otherKey');
});

it('should correctly handle a valid PAT', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(true);
  (getGitlabProjects as jest.Mock).mockResolvedValueOnce({
    projects: [mockGitlabProject()],
    projectsPaging: {
      pageIndex: 1,
      pageSize: 10,
      total: 1
    }
  });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().tokenIsValid).toBe(true);
});

it('should correctly handle an invalid PAT', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(false);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper.state().tokenIsValid).toBe(false);
});

describe('setting a new PAT', () => {
  const routerReplace = jest.fn();
  const wrapper = shallowRender({ router: mockRouter({ replace: routerReplace }) });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should correctly handle it if invalid', async () => {
    (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(false);

    wrapper.instance().handlePersonalAccessTokenCreate('invalidtoken');
    expect(setAlmPersonalAccessToken).toBeCalledWith(almSettingKey, 'invalidtoken');
    expect(wrapper.state().submittingToken).toBe(true);
    await waitAndUpdate(wrapper);
    expect(checkPersonalAccessTokenIsValid).toBeCalled();
    expect(wrapper.state().submittingToken).toBe(false);
    expect(wrapper.state().tokenValidationFailed).toBe(true);
  });

  it('should correctly handle it if valid', async () => {
    (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(true);

    wrapper.instance().handlePersonalAccessTokenCreate('validtoken');
    expect(setAlmPersonalAccessToken).toBeCalledWith(almSettingKey, 'validtoken');
    expect(wrapper.state().submittingToken).toBe(true);
    await waitAndUpdate(wrapper);
    expect(checkPersonalAccessTokenIsValid).toBeCalled();
    expect(wrapper.state().submittingToken).toBe(false);
    expect(wrapper.state().tokenValidationFailed).toBe(false);

    expect(routerReplace).toBeCalled();
  });
});

it('should fetch more projects and preserve search', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(true);

  const projects = [
    mockGitlabProject({ id: '1' }),
    mockGitlabProject({ id: '2' }),
    mockGitlabProject({ id: '3' }),
    mockGitlabProject({ id: '4' }),
    mockGitlabProject({ id: '5' }),
    mockGitlabProject({ id: '6' })
  ];
  (getGitlabProjects as jest.Mock)
    .mockResolvedValueOnce({
      projects: projects.slice(0, 5),
      projectsPaging: {
        pageIndex: 1,
        pageSize: 4,
        total: 6
      }
    })
    .mockResolvedValueOnce({
      projects: projects.slice(5),
      projectsPaging: {
        pageIndex: 2,
        pageSize: 4,
        total: 6
      }
    });

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  wrapper.setState({ searchQuery: 'query' });

  wrapper.instance().handleLoadMore();
  expect(wrapper.state().loadingMore).toBe(true);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loadingMore).toBe(false);
  expect(wrapper.state().projects).toEqual(projects);

  expect(getGitlabProjects).toBeCalledWith(expect.objectContaining({ query: 'query' }));
});

it('should search for projects', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(true);

  const projects = [
    mockGitlabProject({ id: '1' }),
    mockGitlabProject({ id: '2' }),
    mockGitlabProject({ id: '3' }),
    mockGitlabProject({ id: '4' }),
    mockGitlabProject({ id: '5' }),
    mockGitlabProject({ id: '6' })
  ];
  (getGitlabProjects as jest.Mock)
    .mockResolvedValueOnce({
      projects,
      projectsPaging: {
        pageIndex: 1,
        pageSize: 6,
        total: 6
      }
    })
    .mockResolvedValueOnce({
      projects: projects.slice(3, 5),
      projectsPaging: {
        pageIndex: 1,
        pageSize: 6,
        total: 2
      }
    });
  const query = 'query';

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSearch(query);
  expect(wrapper.state().searching).toBe(true);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().searching).toBe(false);
  expect(wrapper.state().searchQuery).toBe(query);
  expect(wrapper.state().projects).toEqual([projects[3], projects[4]]);

  expect(getGitlabProjects).toBeCalledWith(expect.objectContaining({ query }));
});

it('should import', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(true);

  const projects = [mockGitlabProject({ id: '1' }), mockGitlabProject({ id: '2' })];
  (getGitlabProjects as jest.Mock).mockResolvedValueOnce({
    projects,
    projectsPaging: {
      pageIndex: 1,
      pageSize: 6,
      total: 2
    }
  });
  const createdProjectkey = 'imported_project_key';

  (importGitlabProject as jest.Mock).mockResolvedValueOnce({
    project: { key: createdProjectkey }
  });

  const onProjectCreate = jest.fn();

  const wrapper = shallowRender({ onProjectCreate });
  await waitAndUpdate(wrapper);

  wrapper.instance().handleImport(projects[1].id);
  expect(wrapper.state().importingGitlabProjectId).toBe(projects[1].id);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().importingGitlabProjectId).toBeUndefined();
  expect(onProjectCreate).toBeCalledWith([createdProjectkey]);
});

it('should do nothing with missing settings', async () => {
  const wrapper = shallowRender({ settings: [] });

  await waitAndUpdate(wrapper);

  wrapper.instance().handleLoadMore();
  wrapper.instance().handleSearch('whatever');
  wrapper.instance().handlePersonalAccessTokenCreate('token');
  wrapper.instance().handleImport('gitlab project id');

  expect(checkPersonalAccessTokenIsValid).not.toHaveBeenCalled();
  expect(getGitlabProjects).not.toHaveBeenCalled();
  expect(importGitlabProject).not.toHaveBeenCalled();
  expect(setAlmPersonalAccessToken).not.toHaveBeenCalled();
});

it('should handle errors when fetching projects', async () => {
  (getGitlabProjects as jest.Mock).mockRejectedValueOnce({});

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().tokenIsValid).toBe(false);
});

it('should handle errors when importing a project', async () => {
  (importGitlabProject as jest.Mock).mockRejectedValueOnce({});
  (getGitlabProjects as jest.Mock).mockResolvedValueOnce({
    projects: [mockGitlabProject()],
    projectsPaging: {
      pageIndex: 1,
      pageSize: 10,
      total: 1
    }
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().tokenIsValid).toBe(true);

  await wrapper.instance().handleImport('gitlabId');
  await waitAndUpdate(wrapper);

  expect(wrapper.state().tokenIsValid).toBe(false);
});

function shallowRender(props: Partial<GitlabProjectCreate['props']> = {}) {
  return shallow<GitlabProjectCreate>(
    <GitlabProjectCreate
      canAdmin={false}
      loadingBindings={false}
      location={mockLocation()}
      onProjectCreate={jest.fn()}
      router={mockRouter()}
      settings={[mockAlmSettingsInstance({ alm: AlmKeys.GitLab, key: almSettingKey })]}
      {...props}
    />
  );
}
