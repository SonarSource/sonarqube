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
  mockProjectAlmBindingResponse,
  mockProjectBitbucketBindingResponse,
  mockProjectBitbucketCloudBindingResponse,
  mockProjectGithubBindingResponse,
} from '../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../types/alm-settings';
import { renderStepContent } from '../../test-utils';
import WebhookStep, { WebhookStepProps } from '../WebhookStep';

it.each([
  [AlmKeys.Azure, mockProjectAlmBindingResponse({ alm: AlmKeys.Azure })],
  [AlmKeys.BitbucketCloud, mockProjectBitbucketCloudBindingResponse()],
  [AlmKeys.BitbucketServer, mockProjectBitbucketBindingResponse()],
  [AlmKeys.GitHub, mockProjectGithubBindingResponse()],
  [AlmKeys.GitLab, mockProjectAlmBindingResponse({ alm: AlmKeys.GitLab })],
])('should render correctly for %s', (alm, projectBinding) => {
  const wrapper = shallowRender({ alm, projectBinding });
  expect(wrapper).toMatchSnapshot('wrapper');
  expect(renderStepContent(wrapper)).toMatchSnapshot('content');
});

function shallowRender(props: Partial<WebhookStepProps> = {}) {
  return shallow<WebhookStepProps>(
    <WebhookStep
      alm={AlmKeys.BitbucketServer}
      almBinding={mockAlmSettingsInstance()}
      branchesEnabled={true}
      finished={false}
      onDone={jest.fn()}
      onOpen={jest.fn()}
      open={false}
      projectBinding={mockProjectBitbucketBindingResponse()}
      {...props}
    />
  );
}
