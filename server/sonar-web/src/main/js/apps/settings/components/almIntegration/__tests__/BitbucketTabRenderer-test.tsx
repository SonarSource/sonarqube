/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { mockBitbucketBindingDefinition } from '../../../../../helpers/mocks/alm-settings';
import { AlmKeys, BitbucketBindingDefinition } from '../../../../../types/alm-settings';
import AlmTabRenderer, { AlmTabRendererProps } from '../AlmTabRenderer';
import BitbucketTabRenderer, { BitbucketTabRendererProps } from '../BitbucketTabRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ variant: AlmKeys.BitbucketServer })).toMatchSnapshot('bitbucket server');
  expect(shallowRender({ variant: AlmKeys.BitbucketCloud })).toMatchSnapshot('bitbucket cloud');

  const almTab = shallowRender().find<AlmTabRendererProps<BitbucketBindingDefinition>>(
    AlmTabRenderer
  );
  expect(
    almTab.props().form({ formData: mockBitbucketBindingDefinition(), onFieldChange: jest.fn() })
  ).toMatchSnapshot('bitbucket form');
});

function shallowRender(props: Partial<BitbucketTabRendererProps> = {}) {
  return shallow<BitbucketTabRendererProps>(
    <BitbucketTabRenderer
      branchesEnabled={true}
      definitions={[]}
      definitionStatus={{}}
      isCreating={false}
      loadingAlmDefinitions={false}
      loadingProjectCount={false}
      multipleAlmEnabled={true}
      onCancel={jest.fn()}
      onCheck={jest.fn()}
      onCreate={jest.fn()}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      onSelectVariant={jest.fn()}
      onSubmit={jest.fn()}
      submitting={true}
      success={false}
      variant={undefined}
      {...props}
    />
  );
}
