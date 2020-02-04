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
import { AlmKeys } from '../../../../../types/alm-settings';
import AlmIntegrationRenderer, { AlmIntegrationRendererProps } from '../AlmIntegrationRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ definitionKeyForDeletion: 'keyToDelete' })).toMatchSnapshot(
    'delete modal'
  );
  expect(shallowRender({ currentAlm: AlmKeys.Azure })).toMatchSnapshot('azure');
  expect(shallowRender({ currentAlm: AlmKeys.Bitbucket })).toMatchSnapshot('bitbucket');
  expect(shallowRender({ currentAlm: AlmKeys.GitLab })).toMatchSnapshot('gitlab');
});

function shallowRender(props: Partial<AlmIntegrationRendererProps> = {}) {
  return shallow(
    <AlmIntegrationRenderer
      branchesEnabled={true}
      currentAlm={AlmKeys.GitHub}
      definitions={{ azure: [], bitbucket: [], github: [], gitlab: [] }}
      loading={false}
      multipleAlmEnabled={false}
      onCancel={jest.fn()}
      onConfirmDelete={jest.fn()}
      onDelete={jest.fn()}
      onSelectAlm={jest.fn()}
      onUpdateDefinitions={jest.fn()}
      {...props}
    />
  );
}
