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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { mockComponent } from '../../../../helpers/testMocks';
import RenderOptions from '../../components/RenderOptions';
import { renderStepContent } from '../../jenkins/test-utils';
import { BuildTools } from '../../types';
import ProjectKeyStep, { ProjectKeyStepProps } from '../ProjectKeyStep';
import { GITLAB_BUILDTOOLS_LIST } from '../types';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('Step wrapper');
  expect(renderStepContent(wrapper)).toMatchSnapshot('initial content');
});

it.each(GITLAB_BUILDTOOLS_LIST.map(tool => [tool]))(
  'should render correctly for build tool %s',
  buildTool => {
    expect(renderStepContent(shallowRender({ buildTool }))).toMatchSnapshot();
  }
);

it('should correctly callback with selected build tool', () => {
  const setBuildTool = jest.fn();
  const wrapper = shallowRender({ setBuildTool });
  selectBuildTool(wrapper, BuildTools.Maven);

  expect(setBuildTool).toBeCalledWith(BuildTools.Maven);
});

function selectBuildTool(wrapper: ShallowWrapper<ProjectKeyStepProps>, tool: BuildTools) {
  const content = new ShallowWrapper(renderStepContent(wrapper) as JSX.Element);
  content
    .find(RenderOptions)
    .props()
    .onCheck(tool);
}

function shallowRender(props: Partial<ProjectKeyStepProps> = {}) {
  return shallow<ProjectKeyStepProps>(
    <ProjectKeyStep
      component={mockComponent()}
      finished={false}
      onDone={jest.fn()}
      onOpen={jest.fn()}
      open={true}
      setBuildTool={jest.fn()}
      {...props}
    />
  );
}
