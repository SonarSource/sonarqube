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
  getBitbucketServerProjects,
  getBitbucketServerRepositories,
  importBitbucketServerProject,
  searchForBitbucketServerRepositories
} from '../../../../api/alm-integrations';
import {
  mockBitbucketProject,
  mockBitbucketRepository
} from '../../../../helpers/mocks/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import BitbucketProjectCreate from '../BitbucketProjectCreate';

jest.mock('../../../../api/alm-integrations', () => {
  const { mockBitbucketProject, mockBitbucketRepository } = jest.requireActual(
    '../../../../helpers/mocks/alm-integrations'
  );
  return {
    getBitbucketServerProjects: jest.fn().mockResolvedValue({
      projects: [
        mockBitbucketProject({ key: 'project1', name: 'Project 1' }),
        mockBitbucketProject({ id: 2, key: 'project2' })
      ]
    }),
    getBitbucketServerRepositories: jest.fn().mockResolvedValue({
      repositories: [
        mockBitbucketRepository({ projectKey: 'project1' }),
        mockBitbucketRepository({ id: 2, projectKey: 'project1', slug: 'project__repo2' })
      ]
    }),
    importBitbucketServerProject: jest.fn().mockResolvedValue({ project: { key: 'baz' } }),
    searchForBitbucketServerRepositories: jest.fn().mockResolvedValue({
      repositories: [
        mockBitbucketRepository(),
        mockBitbucketRepository({ id: 2, slug: 'project__repo2' })
      ]
    })
  };
});

beforeEach(jest.clearAllMocks);

it('should render correctly', async () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ bitbucketSettings: [] })).toMatchSnapshot('No setting');

  const wrapper = shallowRender();
  (getBitbucketServerRepositories as jest.Mock).mockRejectedValueOnce({});
  await wrapper.instance().handlePersonalAccessTokenCreated();
  expect(wrapper).toMatchSnapshot('No repository');
});

it('should correctly fetch projects and repos', async () => {
  const wrapper = shallowRender();
  await wrapper.instance().handlePersonalAccessTokenCreated();

  // Opens first project on mount.
  expect(getBitbucketServerProjects).toBeCalledWith('foo');
  expect(wrapper.state().projects).toHaveLength(2);

  // Check repos got loaded.
  await waitAndUpdate(wrapper);
  expect(getBitbucketServerRepositories).toBeCalledWith('foo', 'Project 1');
  expect(wrapper.state().projectRepositories).toEqual(
    expect.objectContaining({
      project1: expect.objectContaining({
        repositories: expect.arrayContaining([
          expect.objectContaining({ id: 1 }),
          expect.objectContaining({ id: 2 })
        ])
      })
    })
  );
  expect(wrapper.state().projectRepositories).toBeDefined();
});

it('should correctly import a repo', async () => {
  const onProjectCreate = jest.fn();
  const repo = mockBitbucketRepository();
  const wrapper = shallowRender({ onProjectCreate });
  const instance = wrapper.instance();

  instance.handleSelectRepository(repo);
  instance.handleImportRepository();
  expect(importBitbucketServerProject).toBeCalledWith('foo', repo.projectKey, repo.slug);
  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith('baz');
});

it('should correctly handle search', async () => {
  const wrapper = shallowRender();
  const instance = wrapper.instance();

  // Don't trigger search on empty query.
  instance.handleSearch('');
  expect(searchForBitbucketServerRepositories).not.toBeCalled();
  expect(wrapper.state().searching).toBe(false);
  expect(wrapper.state().searchResults).toBeUndefined();

  instance.handleSearch('bar');
  expect(searchForBitbucketServerRepositories).toBeCalledWith('foo', 'bar');
  expect(wrapper.state().searching).toBe(true);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().searching).toBe(false);
  expect(wrapper.state().searchResults).toHaveLength(2);
});

it('should behave correctly when no setting', async () => {
  const wrapper = shallowRender({ bitbucketSettings: [] });
  await wrapper.instance().handleSearch('');
  await wrapper.instance().handleImportRepository();
  await wrapper.instance().fetchBitbucketRepositories([mockBitbucketProject()]);

  expect(searchForBitbucketServerRepositories).not.toHaveBeenCalled();
  expect(importBitbucketServerProject).not.toHaveBeenCalled();
  expect(getBitbucketServerRepositories).not.toHaveBeenCalled();
});

function shallowRender(props: Partial<BitbucketProjectCreate['props']> = {}) {
  return shallow<BitbucketProjectCreate>(
    <BitbucketProjectCreate
      canAdmin={false}
      bitbucketSettings={[mockAlmSettingsInstance({ alm: AlmKeys.BitbucketServer, key: 'foo' })]}
      loadingBindings={false}
      location={mockLocation()}
      router={mockRouter()}
      onProjectCreate={jest.fn()}
      {...props}
    />
  );
}
