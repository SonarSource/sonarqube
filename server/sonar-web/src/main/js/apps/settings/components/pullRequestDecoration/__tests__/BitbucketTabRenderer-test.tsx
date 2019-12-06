/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { mockBitbucketDefinition } from '../../../../../helpers/mocks/alm-settings';
import BitbucketTabRenderer, { BitbucketTabRendererProps } from '../BitbucketTabRenderer';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot();
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ editedDefinition: mockBitbucketDefinition() })).toMatchSnapshot();
});

function shallowRender(props: Partial<BitbucketTabRendererProps> = {}) {
  return shallow(
    <BitbucketTabRenderer
      definitions={[]}
      loading={false}
      onCancel={jest.fn()}
      onCreate={jest.fn()}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      onSubmit={jest.fn()}
      {...props}
    />
  );
}
