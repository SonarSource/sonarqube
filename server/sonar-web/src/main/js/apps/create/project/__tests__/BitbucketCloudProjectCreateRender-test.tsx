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
import { mockBitbucketCloudAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import BitbucketCloudProjectCreateRenderer, {
  BitbucketCloudProjectCreateRendererProps
} from '../BitbucketCloudProjectCreateRender';

it('Should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ settings: undefined })).toMatchSnapshot('Wrong config');
  expect(shallowRender({ loading: true })).toMatchSnapshot('Loading...');
  expect(
    shallowRender({
      showPersonalAccessTokenForm: true
    })
  ).toMatchSnapshot('Need App password');
});

function shallowRender(props?: Partial<BitbucketCloudProjectCreateRendererProps>) {
  return shallow(
    <BitbucketCloudProjectCreateRenderer
      onImport={jest.fn()}
      isLastPage={true}
      loading={false}
      loadingMore={false}
      onLoadMore={jest.fn()}
      onPersonalAccessTokenCreated={jest.fn()}
      onSearch={jest.fn()}
      resetPat={false}
      searching={false}
      searchQuery=""
      settings={mockBitbucketCloudAlmSettingsInstance()}
      showPersonalAccessTokenForm={false}
      {...props}
    />
  );
}
