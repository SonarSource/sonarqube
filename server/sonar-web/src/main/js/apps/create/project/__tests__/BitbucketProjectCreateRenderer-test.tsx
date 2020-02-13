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
import {
  mockBitbucketProject,
  mockBitbucketRepository
} from '../../../../helpers/mocks/alm-integrations';
import { mockAlmSettingsInstance } from '../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../types/alm-settings';
import BitbucketProjectCreateRenderer, {
  BitbucketProjectCreateRendererProps
} from '../BitbucketProjectCreateRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ showPersonalAccessTokenForm: true })).toMatchSnapshot('pat form');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ importing: true })).toMatchSnapshot('importing');
  expect(shallowRender({ selectedRepository: mockBitbucketRepository() })).toMatchSnapshot(
    'selected repo'
  );
  expect(shallowRender({ bitbucketSetting: undefined })).toMatchSnapshot(
    'invalid config, regular user'
  );
  expect(shallowRender({ bitbucketSetting: undefined, canAdmin: true })).toMatchSnapshot(
    'invalid config, admin user'
  );
});

function shallowRender(props: Partial<BitbucketProjectCreateRendererProps> = {}) {
  return shallow<BitbucketProjectCreateRendererProps>(
    <BitbucketProjectCreateRenderer
      bitbucketSetting={mockAlmSettingsInstance({ alm: AlmKeys.Bitbucket })}
      importing={false}
      loading={false}
      onImportRepository={jest.fn()}
      onPersonalAccessTokenCreate={jest.fn()}
      onProjectCreate={jest.fn()}
      onSearch={jest.fn()}
      onSelectRepository={jest.fn()}
      projectRepositories={{ foo: { allShown: true, repositories: [mockBitbucketRepository()] } }}
      projects={[mockBitbucketProject({ key: 'foo' })]}
      searching={false}
      tokenValidationFailed={false}
      {...props}
    />
  );
}
