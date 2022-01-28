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
import { getGitlabProjects, importGitlabProject } from '../../../../api/alm-integrations';
import { mockGitlabProject } from '../../../../helpers/mocks/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import GitlabProjectCreate from '../GitlabProjectCreate';

jest.mock('../../../../api/alm-integrations', () => ({
  getGitlabProjects: jest.fn().mockRejectedValue('error'),
  importGitlabProject: jest.fn().mockRejectedValue('error')
}));

beforeEach(jest.clearAllMocks);

const almSettingKey = 'gitlab-setting';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should fetch more projects and preserve search', async () => {
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

  await wrapper.instance().handlePersonalAccessTokenCreated();
  wrapper.setState({ searchQuery: 'query' });

  wrapper.instance().handleLoadMore();
  expect(wrapper.state().loadingMore).toBe(true);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loadingMore).toBe(false);
  expect(wrapper.state().projects).toEqual(projects);

  expect(getGitlabProjects).toBeCalledWith(expect.objectContaining({ query: 'query' }));
});

it('should search for projects', async () => {
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
  await wrapper.instance().handlePersonalAccessTokenCreated();

  wrapper.instance().handleSearch(query);
  expect(wrapper.state().searching).toBe(true);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().searching).toBe(false);
  expect(wrapper.state().searchQuery).toBe(query);
  expect(wrapper.state().projects).toEqual([projects[3], projects[4]]);

  expect(getGitlabProjects).toBeCalledWith(expect.objectContaining({ query }));
});

it('should import', async () => {
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
  await wrapper.instance().handlePersonalAccessTokenCreated();

  wrapper.instance().handleImport(projects[1].id);
  expect(wrapper.state().importingGitlabProjectId).toBe(projects[1].id);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().importingGitlabProjectId).toBeUndefined();
  expect(onProjectCreate).toBeCalledWith(createdProjectkey);
});

it('should do nothing with missing settings', async () => {
  const wrapper = shallowRender({ settings: [] });

  await wrapper.instance().handleLoadMore();
  await wrapper.instance().handleSearch('whatever');
  await wrapper.instance().handlePersonalAccessTokenCreated();
  await wrapper.instance().handleImport('gitlab project id');

  expect(getGitlabProjects).not.toHaveBeenCalled();
  expect(importGitlabProject).not.toHaveBeenCalled();
});

it('should handle errors when fetching projects', async () => {
  (getGitlabProjects as jest.Mock).mockRejectedValueOnce({});

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  await wrapper.instance().handlePersonalAccessTokenCreated();

  expect(wrapper.state().resetPat).toBe(true);
  expect(wrapper.state().showPersonalAccessTokenForm).toBe(true);
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
  await wrapper.instance().handlePersonalAccessTokenCreated();

  await wrapper.instance().handleImport('gitlabId');
  await waitAndUpdate(wrapper);

  expect(wrapper.state().showPersonalAccessTokenForm).toBe(true);
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
