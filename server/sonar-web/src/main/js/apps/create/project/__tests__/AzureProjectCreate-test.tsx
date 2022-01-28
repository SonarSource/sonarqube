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
import {
  checkPersonalAccessTokenIsValid,
  getAzureProjects,
  getAzureRepositories,
  importAzureRepository,
  searchAzureRepositories,
  setAlmPersonalAccessToken
} from '../../../../api/alm-integrations';
import { mockAzureProject, mockAzureRepository } from '../../../../helpers/mocks/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import AzureProjectCreate from '../AzureProjectCreate';

jest.mock('../../../../api/alm-integrations', () => {
  return {
    checkPersonalAccessTokenIsValid: jest.fn().mockResolvedValue({ status: true }),
    setAlmPersonalAccessToken: jest.fn().mockResolvedValue(null),
    getAzureProjects: jest.fn().mockResolvedValue({ projects: [] }),
    getAzureRepositories: jest.fn().mockResolvedValue({ repositories: [] }),
    searchAzureRepositories: jest.fn().mockResolvedValue({ repositories: [] }),
    importAzureRepository: jest.fn().mockResolvedValue({ project: { key: 'baz' } })
  };
});

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should correctly fetch binding info on mount', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalledWith('foo');
});

it('should correctly handle a valid PAT', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce({ status: true });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().patIsValid).toBe(true);
});

it('should correctly handle an invalid PAT', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce({ status: false });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().patIsValid).toBe(false);
});

it('should correctly handle setting a new PAT', async () => {
  const router = mockRouter();
  const wrapper = shallowRender({ router });
  wrapper.instance().handlePersonalAccessTokenCreate('token');
  expect(setAlmPersonalAccessToken).toBeCalledWith('foo', 'token');
  expect(wrapper.state().submittingToken).toBe(true);

  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce({ status: false });
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().submittingToken).toBe(false);
  expect(wrapper.state().tokenValidationFailed).toBe(true);

  // Try again, this time with a correct token:

  wrapper.instance().handlePersonalAccessTokenCreate('correct token');
  await waitAndUpdate(wrapper);
  expect(wrapper.state().tokenValidationFailed).toBe(false);
  expect(router.replace).toBeCalled();
});

it('should correctly fetch projects and repositories on mount', async () => {
  const project = mockAzureProject();
  (getAzureProjects as jest.Mock).mockResolvedValueOnce({ projects: [project] });
  (getAzureRepositories as jest.Mock).mockResolvedValueOnce({
    repositories: [mockAzureRepository()]
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getAzureProjects).toBeCalled();
  expect(getAzureRepositories).toBeCalledTimes(1);
  expect(getAzureRepositories).toBeCalledWith('foo', project.name);
});

it('should handle opening a project', async () => {
  const projects = [
    mockAzureProject(),
    mockAzureProject({ name: 'project2', description: 'Project to open' })
  ];

  const firstProjectRepos = [mockAzureRepository()];
  const secondProjectRepos = [mockAzureRepository({ projectName: projects[1].name })];

  (getAzureProjects as jest.Mock).mockResolvedValueOnce({ projects });
  (getAzureRepositories as jest.Mock)
    .mockResolvedValueOnce({
      repositories: firstProjectRepos
    })
    .mockResolvedValueOnce({
      repositories: secondProjectRepos
    });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().handleOpenProject(projects[1].name);
  await waitAndUpdate(wrapper);

  expect(getAzureRepositories).toBeCalledWith('foo', projects[1].name);

  expect(wrapper.state().repositories).toEqual({
    [projects[0].name]: firstProjectRepos,
    [projects[1].name]: secondProjectRepos
  });
});

it('should handle searching for repositories', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  const query = 'repo';
  const repositories = [mockAzureRepository({ projectName: 'p2' })];
  (searchAzureRepositories as jest.Mock).mockResolvedValueOnce({
    repositories
  });
  wrapper.instance().handleSearchRepositories(query);
  expect(wrapper.state().searching).toBe(true);

  expect(searchAzureRepositories).toBeCalledWith('foo', query);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().searching).toBe(false);
  expect(wrapper.state().searchResults).toEqual({ [repositories[0].projectName]: repositories });
  expect(wrapper.state().searchQuery).toBe(query);

  // Ignore opening a project when search results are displayed
  (getAzureRepositories as jest.Mock).mockClear();
  wrapper.instance().handleOpenProject('whatever');
  expect(getAzureRepositories).not.toHaveBeenCalled();

  // and reset the search field
  (searchAzureRepositories as jest.Mock).mockClear();

  wrapper.instance().handleSearchRepositories('');
  expect(searchAzureRepositories).not.toBeCalled();
  expect(wrapper.state().searchResults).toBeUndefined();
  expect(wrapper.state().searchQuery).toBeUndefined();
});

it('should select and import a repository', async () => {
  const onProjectCreate = jest.fn();
  const repository = mockAzureRepository();
  const wrapper = shallowRender({ onProjectCreate });
  await waitAndUpdate(wrapper);

  expect(wrapper.state().selectedRepository).toBeUndefined();
  wrapper.instance().handleSelectRepository(repository);
  expect(wrapper.state().selectedRepository).toBe(repository);

  wrapper.instance().handleImportRepository();
  expect(wrapper.state().importing).toBe(true);
  expect(importAzureRepository).toBeCalledWith('foo', repository.projectName, repository.name);
  await waitAndUpdate(wrapper);

  expect(onProjectCreate).toBeCalledWith('baz');
  expect(wrapper.state().importing).toBe(false);
});

it('should handle no settings', () => {
  const wrapper = shallowRender({ settings: [] });

  wrapper.instance().fetchAzureProjects();
  wrapper.instance().fetchAzureRepositories('whatever');
  wrapper.instance().handleSearchRepositories('query');
  wrapper.instance().handleImportRepository();
  wrapper.instance().checkPersonalAccessToken();
  wrapper.instance().handlePersonalAccessTokenCreate('');

  expect(getAzureProjects).not.toBeCalled();
  expect(getAzureRepositories).not.toBeCalled();
  expect(searchAzureRepositories).not.toBeCalled();
  expect(importAzureRepository).not.toBeCalled();
  expect(checkPersonalAccessTokenIsValid).not.toBeCalled();
  expect(setAlmPersonalAccessToken).not.toBeCalled();
});

function shallowRender(overrides: Partial<AzureProjectCreate['props']> = {}) {
  return shallow<AzureProjectCreate>(
    <AzureProjectCreate
      canAdmin={true}
      loadingBindings={false}
      location={mockLocation()}
      onProjectCreate={jest.fn()}
      router={mockRouter()}
      settings={[mockAlmSettingsInstance({ alm: AlmKeys.Azure, key: 'foo' })]}
      {...overrides}
    />
  );
}
