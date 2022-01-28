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
import { Button } from '../../../../components/controls/buttons';
import ListFooter from '../../../../components/controls/ListFooter';
import SearchBox from '../../../../components/controls/SearchBox';
import { mockGitlabProject } from '../../../../helpers/mocks/alm-integrations';
import GitlabProjectSelectionForm, {
  GitlabProjectSelectionFormProps
} from '../GitlabProjectSelectionForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('projects');

  expect(shallowRender({ projects: undefined, projectsPaging: mockPaging() })).toMatchSnapshot(
    'undefined projects'
  );
  expect(shallowRender({ projects: [], projectsPaging: mockPaging() })).toMatchSnapshot(
    'no projects'
  );
  expect(
    shallowRender({ projects: [], projectsPaging: mockPaging(), searchQuery: 'findme' })
  ).toMatchSnapshot('no projects when searching');

  expect(shallowRender({ importingGitlabProjectId: '2' })).toMatchSnapshot('importing');
});

describe('appropriate callback', () => {
  const onImport = jest.fn();
  const onLoadMore = jest.fn();
  const onSearch = jest.fn();
  const wrapper = shallowRender({ onImport, onLoadMore, onSearch });

  it('should be called when clicking to import', () => {
    wrapper
      .find(Button)
      .first()
      .simulate('click');

    expect(onImport).toBeCalled();
  });

  it('should be assigned to the list footer', () => {
    const { loadMore } = wrapper
      .find(ListFooter)
      .first()
      .props();

    expect(loadMore).toBe(onLoadMore);
  });

  it('should be assigned to the search box', () => {
    const { onChange } = wrapper
      .find(SearchBox)
      .first()
      .props();

    expect(onChange).toBe(onSearch);
  });
});

function shallowRender(props: Partial<GitlabProjectSelectionFormProps> = {}) {
  const projects = [
    mockGitlabProject(),
    mockGitlabProject({
      id: '2',
      sqProjectKey: 'already-imported',
      sqProjectName: 'Already Imported'
    })
  ];

  return shallow<GitlabProjectSelectionFormProps>(
    <GitlabProjectSelectionForm
      loadingMore={false}
      onImport={jest.fn()}
      onLoadMore={jest.fn()}
      onSearch={jest.fn()}
      projects={projects}
      projectsPaging={mockPaging(projects.length)}
      searching={false}
      searchQuery=""
      {...props}
    />
  );
}

function mockPaging(total = 0) {
  return { total, pageIndex: 1, pageSize: 30 };
}
