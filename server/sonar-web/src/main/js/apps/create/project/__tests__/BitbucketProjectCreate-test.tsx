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
  getBitbucketServerProjects,
  getBitbucketServerRepositories,
  importBitbucketServerProject,
  searchForBitbucketServerRepositories,
  setAlmPersonalAccessToken
} from '../../../../api/alm-integrations';
import { mockBitbucketRepository } from '../../../../helpers/mocks/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { mockLocation } from '../../../../helpers/testMocks';
import { AlmKeys } from '../../../../types/alm-settings';
import { BitbucketProjectCreate } from '../BitbucketProjectCreate';

jest.mock('../../../../api/alm-integrations', () => {
  const { mockBitbucketProject, mockBitbucketRepository } = jest.requireActual(
    '../../../../helpers/mocks/alm-integrations'
  );
  return {
    checkPersonalAccessTokenIsValid: jest.fn().mockResolvedValue(true),
    getBitbucketServerProjects: jest.fn().mockResolvedValue({
      projects: [
        mockBitbucketProject({ key: 'project1', name: 'Project 1' }),
        mockBitbucketProject({ id: 2, key: 'project2' })
      ]
    }),
    getBitbucketServerRepositories: jest.fn().mockResolvedValue({
      repositories: [
        mockBitbucketRepository(),
        mockBitbucketRepository({ id: 2, slug: 'project__repo2' })
      ]
    }),
    importBitbucketServerProject: jest.fn().mockResolvedValue({ project: { key: 'baz' } }),
    setAlmPersonalAccessToken: jest.fn().mockResolvedValue(null),
    searchForBitbucketServerRepositories: jest.fn().mockResolvedValue({
      repositories: [
        mockBitbucketRepository(),
        mockBitbucketRepository({ id: 2, slug: 'project__repo2' })
      ]
    })
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
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(true);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().patIsValid).toBe(true);
});

it('should correctly handle an invalid PAT', async () => {
  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(false);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().patIsValid).toBe(false);
});

it('should correctly handle setting a new PAT', async () => {
  const wrapper = shallowRender();
  wrapper.instance().handlePersonalAccessTokenCreate('token');
  expect(setAlmPersonalAccessToken).toBeCalledWith('foo', 'token');
  expect(wrapper.state().submittingToken).toBe(true);

  (checkPersonalAccessTokenIsValid as jest.Mock).mockResolvedValueOnce(false);
  await waitAndUpdate(wrapper);
  expect(checkPersonalAccessTokenIsValid).toBeCalled();
  expect(wrapper.state().submittingToken).toBe(false);
  expect(wrapper.state().tokenValidationFailed).toBe(true);
});

it('should correctly fetch projects and repos', async () => {
  const wrapper = shallowRender();

  // Opens first project on mount.
  await waitAndUpdate(wrapper);
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
  expect(onProjectCreate).toBeCalledWith(['baz']);
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

function shallowRender(props: Partial<BitbucketProjectCreate['props']> = {}) {
  return shallow<BitbucketProjectCreate>(
    <BitbucketProjectCreate
      bitbucketSettings={[mockAlmSettingsInstance({ alm: AlmKeys.Bitbucket, key: 'foo' })]}
      loadingBindings={false}
      location={mockLocation()}
      onProjectCreate={jest.fn()}
      {...props}
    />
  );
}
