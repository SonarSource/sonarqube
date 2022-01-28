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
import Radio from '../../../../components/controls/Radio';
import SearchBox from '../../../../components/controls/SearchBox';
import SearchSelect from '../../../../components/controls/SearchSelect';
import { mockGitHubRepository } from '../../../../helpers/mocks/alm-integrations';
import { GithubOrganization } from '../../../../types/alm-integration';
import GitHubProjectCreateRenderer, {
  GitHubProjectCreateRendererProps
} from '../GitHubProjectCreateRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loadingBindings: true })).toMatchSnapshot('loading');
  expect(shallowRender({ error: true })).toMatchSnapshot('error');
  expect(shallowRender({ canAdmin: true, error: true })).toMatchSnapshot('error for admin');

  const organizations: GithubOrganization[] = [
    { key: 'o1', name: 'org1' },
    { key: 'o2', name: 'org2' }
  ];

  expect(shallowRender({ organizations })).toMatchSnapshot('organizations');
  expect(
    shallowRender({
      organizations,
      selectedOrganization: organizations[1]
    })
  ).toMatchSnapshot('no repositories');

  const repositories = [
    mockGitHubRepository({ id: '1', key: 'repo1' }),
    mockGitHubRepository({ id: '2', key: 'repo2', sqProjectKey: 'repo2' }),
    mockGitHubRepository({ id: '3', key: 'repo3' })
  ];

  expect(
    shallowRender({
      organizations,
      selectedOrganization: organizations[1],
      repositories,
      selectedRepository: repositories[2]
    })
  ).toMatchSnapshot('repositories');
});

describe('callback', () => {
  const onImportRepository = jest.fn();
  const onSelectOrganization = jest.fn();
  const onSelectRepository = jest.fn();
  const onSearch = jest.fn();
  const org = { key: 'o1', name: 'org' };
  const wrapper = shallowRender({
    onImportRepository,
    onSelectOrganization,
    onSelectRepository,
    onSearch,
    organizations: [org],
    selectedOrganization: org,
    repositories: [mockGitHubRepository()]
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should be called when org is selected', () => {
    const value = 'o1';
    wrapper.find(SearchSelect).simulate('select', { value });
    expect(onSelectOrganization).toBeCalledWith(value);
  });

  it('should be called when searchbox is changed', () => {
    const value = 'search query';
    wrapper
      .find(SearchBox)
      .props()
      .onChange(value);
    expect(onSearch).toBeCalledWith(value);
  });

  it('should be called when repo is selected', () => {
    const value = 'repo1';
    wrapper
      .find(Radio)
      .props()
      .onCheck(value);
    expect(onSelectRepository).toBeCalledWith(value);
  });
});

function shallowRender(props: Partial<GitHubProjectCreateRendererProps> = {}) {
  return shallow<GitHubProjectCreateRendererProps>(
    <GitHubProjectCreateRenderer
      canAdmin={false}
      error={false}
      importing={false}
      loadingBindings={false}
      loadingOrganizations={false}
      loadingRepositories={false}
      onImportRepository={jest.fn()}
      onLoadMore={jest.fn()}
      onSearch={jest.fn()}
      onSelectOrganization={jest.fn()}
      onSelectRepository={jest.fn()}
      organizations={[]}
      repositoryPaging={{ total: 0, pageIndex: 1, pageSize: 30 }}
      searchQuery=""
      {...props}
    />
  );
}
