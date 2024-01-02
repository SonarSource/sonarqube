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
import { mockSetOfBranchAndPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import BranchLikeTabs from '../BranchLikeTabs';
import { ProjectBranchesApp, ProjectBranchesAppProps } from '../ProjectBranchesApp';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should properly notify that a branch or a pr has been changed/deleted', () => {
  const onBranchesChange = jest.fn();
  const wrapper = shallowRender({ onBranchesChange });

  wrapper.find(BranchLikeTabs).props().onBranchesChange();

  expect(onBranchesChange).toHaveBeenCalled();
});

function shallowRender(props?: Partial<ProjectBranchesAppProps>) {
  return shallow(
    <ProjectBranchesApp
      branchLikes={mockSetOfBranchAndPullRequest()}
      component={mockComponent()}
      onBranchesChange={jest.fn()}
      {...props}
    />
  );
}
