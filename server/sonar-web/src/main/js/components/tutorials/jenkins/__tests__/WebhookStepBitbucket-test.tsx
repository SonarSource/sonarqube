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
import {
  mockAlmSettingsInstance,
  mockProjectBitbucketBindingResponse,
  mockProjectBitbucketCloudBindingResponse,
} from '../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../types/alm-settings';
import WebhookStepBitbucket, { WebhookStepBitbucketProps } from '../WebhookStepBitbucket';

it.each([
  [
    AlmKeys.BitbucketServer,
    mockProjectBitbucketBindingResponse(),
    mockAlmSettingsInstance({ url: 'http://bbs.enterprise.com' }),
  ],
  [
    AlmKeys.BitbucketCloud,
    mockProjectBitbucketCloudBindingResponse(),
    mockAlmSettingsInstance({ url: 'http://bitbucket.org/workspace/' }),
  ],
])('should render correctly for %s', (alm, projectBinding, almBinding) => {
  expect(shallowRender({ alm, projectBinding, almBinding })).toMatchSnapshot();
  expect(shallowRender({ alm, projectBinding, almBinding: undefined })).toMatchSnapshot(
    'with no alm binding'
  );
  expect(
    shallowRender({ alm, projectBinding, almBinding, branchesEnabled: false })
  ).toMatchSnapshot('with branches disabled');
});

function shallowRender(props: Partial<WebhookStepBitbucketProps> = {}) {
  return shallow<WebhookStepBitbucketProps>(
    <WebhookStepBitbucket
      alm={AlmKeys.BitbucketServer}
      branchesEnabled={true}
      projectBinding={mockProjectBitbucketBindingResponse()}
      {...props}
    />
  );
}
