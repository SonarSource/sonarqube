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
  mockAzureBindingDefinition,
  mockBitbucketCloudBindingDefinition,
  mockGithubBindingDefinition,
} from '../../../../../helpers/mocks/alm-settings';
import { AlmKeys } from '../../../../../types/alm-settings';
import AlmTabRenderer, { AlmTabRendererProps } from '../AlmTabRenderer';

it('should render correctly for multi-ALM binding', () => {
  expect(shallowRenderAzure({ loadingAlmDefinitions: true })).toMatchSnapshot(
    'loading ALM definitions'
  );
  expect(shallowRenderAzure({ loadingProjectCount: true })).toMatchSnapshot(
    'loading project count'
  );
  expect(shallowRenderAzure({})).toMatchSnapshot('loaded');
  expect(shallowRenderAzure({ editedDefinition: mockAzureBindingDefinition() })).toMatchSnapshot(
    'editing a definition'
  );
});

it('should render correctly for single-ALM binding', () => {
  expect(
    shallowRenderAzure({ loadingAlmDefinitions: true, multipleAlmEnabled: false })
  ).toMatchSnapshot();
  expect(shallowRenderAzure({ multipleAlmEnabled: false })).toMatchSnapshot();
  expect(
    shallowRenderAzure({ definitions: [mockAzureBindingDefinition()], multipleAlmEnabled: false })
  ).toMatchSnapshot();
});

it('should render correctly with validation', () => {
  const githubProps = {
    alm: AlmKeys.GitHub,
    definitions: [mockGithubBindingDefinition()],
  };
  expect(shallowRender(githubProps)).toMatchSnapshot('default');
  expect(shallowRender({ ...githubProps, definitions: [] })).toMatchSnapshot('empty');

  expect(
    shallowRender({
      ...githubProps,
      editedDefinition: mockGithubBindingDefinition(),
    })
  ).toMatchSnapshot('create a second');

  expect(
    shallowRender({
      ...githubProps,
      definitions: [],
      editedDefinition: mockGithubBindingDefinition(),
    })
  ).toMatchSnapshot('create a first');

  expect(
    shallowRender({
      almTab: AlmKeys.BitbucketServer, // BitbucketServer will be passed for both Bitbucket variants.
      definitions: [mockBitbucketCloudBindingDefinition()],
    })
  ).toMatchSnapshot('pass the correct key for bitbucket cloud');
});

function shallowRenderAzure(props: Partial<AlmTabRendererProps>) {
  return shallowRender({
    definitions: [mockAzureBindingDefinition()],
    ...props,
  });
}

function shallowRender(props: Partial<AlmTabRendererProps> = {}) {
  return shallow(
    <AlmTabRenderer
      almTab={AlmKeys.Azure}
      branchesEnabled={true}
      definitions={[]}
      definitionStatus={{}}
      loadingAlmDefinitions={false}
      loadingProjectCount={false}
      multipleAlmEnabled={true}
      onCancel={jest.fn()}
      onCheck={jest.fn()}
      onCreate={jest.fn()}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      afterSubmit={jest.fn()}
      {...props}
    />
  );
}
