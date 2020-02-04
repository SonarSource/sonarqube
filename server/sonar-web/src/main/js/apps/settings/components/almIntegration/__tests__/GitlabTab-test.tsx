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
import { mockGitlabDefinition } from '../../../../../helpers/mocks/alm-settings';
import GitlabTab, { GitlabTabProps } from '../GitlabTab';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('with branch support');
  expect(shallowRender({ branchesEnabled: false })).toMatchSnapshot('without branch support');
});

function shallowRender(props: Partial<GitlabTabProps> = {}) {
  return shallow(
    <GitlabTab
      branchesEnabled={true}
      definitions={[mockGitlabDefinition()]}
      loading={false}
      multipleAlmEnabled={true}
      onDelete={jest.fn()}
      onUpdateDefinitions={jest.fn()}
      {...props}
    />
  );
}
