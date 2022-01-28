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
import SearchBox from '../../../../components/controls/SearchBox';
import {
  mockBitbucketProject,
  mockBitbucketRepository
} from '../../../../helpers/mocks/alm-integrations';
import { change } from '../../../../helpers/testUtils';
import BitbucketImportRepositoryForm, {
  BitbucketImportRepositoryFormProps
} from '../BitbucketImportRepositoryForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ projects: undefined })).toMatchSnapshot('no projects');
  expect(shallowRender({ searching: true })).toMatchSnapshot('searching');
  expect(shallowRender({ searchResults: [mockBitbucketRepository()] })).toMatchSnapshot(
    'search results'
  );
});

it('should correctly handle search', () => {
  const onSearch = jest.fn();
  const wrapper = shallowRender({ onSearch });
  change(wrapper.find(SearchBox), 'foo');
  expect(onSearch).toBeCalledWith('foo');
});

function shallowRender(props: Partial<BitbucketImportRepositoryFormProps> = {}) {
  return shallow<BitbucketImportRepositoryFormProps>(
    <BitbucketImportRepositoryForm
      disableRepositories={false}
      onSearch={jest.fn()}
      onSelectRepository={jest.fn()}
      projectRepositories={{
        project: {
          allShown: true,
          repositories: [
            mockBitbucketRepository(),
            mockBitbucketRepository({ id: 2, slug: 'bar', name: 'Bar', sqProjectKey: 'bar' })
          ]
        }
      }}
      projects={[
        mockBitbucketProject(),
        mockBitbucketProject({ id: 2, key: 'project2', name: 'Project 2' })
      ]}
      searching={false}
      {...props}
    />
  );
}
