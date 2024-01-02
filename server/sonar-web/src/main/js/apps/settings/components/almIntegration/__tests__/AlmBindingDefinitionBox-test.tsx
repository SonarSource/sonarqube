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
  mockAlmSettingsBindingStatus,
  mockAzureBindingDefinition,
  mockBitbucketCloudBindingDefinition,
  mockGithubBindingDefinition,
  mockGitlabBindingDefinition,
} from '../../../../../helpers/mocks/alm-settings';
import { AlmKeys, AlmSettingsBindingStatusType } from '../../../../../types/alm-settings';
import AlmBindingDefinitionBox, { AlmBindingDefinitionBoxProps } from '../AlmBindingDefinitionBox';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({
      status: mockAlmSettingsBindingStatus({
        type: AlmSettingsBindingStatusType.Success,
      }),
    })
  ).toMatchSnapshot('success');
  expect(
    shallowRender({
      status: mockAlmSettingsBindingStatus({
        failureMessage: 'Oops, something went wrong',
        type: AlmSettingsBindingStatusType.Failure,
      }),
    })
  ).toMatchSnapshot('error');

  expect(
    shallowRender({
      status: mockAlmSettingsBindingStatus({
        alertSuccess: true,
        type: AlmSettingsBindingStatusType.Success,
      }),
    })
  ).toMatchSnapshot('success with alert');

  expect(
    shallowRender({
      status: mockAlmSettingsBindingStatus({
        type: AlmSettingsBindingStatusType.Warning,
      }),
    })
  ).toMatchSnapshot('warning');

  expect(
    shallowRender({ alm: AlmKeys.Azure, definition: mockAzureBindingDefinition() })
  ).toMatchSnapshot('Azure DevOps');

  expect(
    shallowRender({
      status: mockAlmSettingsBindingStatus({
        type: AlmSettingsBindingStatusType.Success,
      }),
      alm: AlmKeys.GitLab,
      definition: mockGitlabBindingDefinition(),
    })
  ).toMatchSnapshot('success for GitLab');

  expect(
    shallowRender({
      status: mockAlmSettingsBindingStatus({
        type: AlmSettingsBindingStatusType.Success,
      }),
      alm: AlmKeys.BitbucketCloud,
      definition: mockBitbucketCloudBindingDefinition(),
    })
  ).toMatchSnapshot('success for Bitbucket Cloud');

  expect(
    shallowRender({
      branchesEnabled: false,
      status: mockAlmSettingsBindingStatus({
        alertSuccess: true,
        type: AlmSettingsBindingStatusType.Success,
      }),
    })
  ).toMatchSnapshot('success with branches disabled');
});

function shallowRender(props: Partial<AlmBindingDefinitionBoxProps> = {}) {
  return shallow(
    <AlmBindingDefinitionBox
      alm={AlmKeys.GitHub}
      branchesEnabled={true}
      definition={mockGithubBindingDefinition()}
      onCheck={jest.fn()}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      {...props}
    />
  );
}
