/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { mockAlmSettingsInstance } from '../../../../../helpers/mocks/alm-settings';
import { AlmKeys, AlmSettingsInstance } from '../../../../../types/alm-settings';
import { AlmSpecificForm, AlmSpecificFormProps } from '../AlmSpecificForm';

it.each([
  [AlmKeys.Azure],
  [AlmKeys.BitbucketServer],
  [AlmKeys.BitbucketCloud],
  [AlmKeys.GitHub],
  [AlmKeys.GitLab],
])('should render correctly for %s', (alm) => {
  expect(shallowRender(alm)).toMatchSnapshot();
});

it.each([
  [
    AlmKeys.BitbucketServer,
    [mockAlmSettingsInstance({ alm: AlmKeys.BitbucketServer, url: 'http://bbs.example.com' })],
  ],
  [AlmKeys.GitHub, [mockAlmSettingsInstance({ url: 'http://example.com/api/v3' })]],
  [AlmKeys.GitHub, [mockAlmSettingsInstance({ url: 'http://api.github.com' })]],
])(
  'should render correctly for %s if an instance URL is provided',
  (alm: AlmKeys, instances: AlmSettingsInstance[]) => {
    expect(shallowRender(alm, { instances })).toMatchSnapshot();
  }
);

it('should render the monorepo field when the feature is supported', () => {
  expect(shallowRender(AlmKeys.Azure, { hasFeature: jest.fn(() => true) })).toMatchSnapshot();
});

function shallowRender(alm: AlmKeys, props: Partial<AlmSpecificFormProps> = {}) {
  return shallow(
    <AlmSpecificForm
      alm={alm}
      instances={[]}
      formData={{
        key: '',
        repository: '',
        slug: '',
        monorepo: false,
      }}
      onFieldChange={jest.fn()}
      hasFeature={jest.fn()}
      {...props}
    />
  );
}
