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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import RenderOptions from '../../components/RenderOptions';
import { renderStepContent } from '../../test-utils';
import { BuildTools } from '../../types';
import { ProjectKeyStep, ProjectKeyStepProps } from '../ProjectKeyStep';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('Step wrapper');
  expect(renderStepContent(wrapper)).toMatchSnapshot('initial content');
});

it('should render correctly if C is not available', () => {
  const wrapper = shallowRender({ hasCLanguageFeature: false });
  expect(renderStepContent(wrapper)).toMatchSnapshot();
});

it.each([
  [BuildTools.Maven],
  [BuildTools.Gradle],
  [BuildTools.DotNet],
  [BuildTools.CFamily],
  [BuildTools.Other],
])('should render correctly for build tool %s', (buildTool) => {
  expect(renderStepContent(shallowRender({ buildTool }))).toMatchSnapshot();
});

it('should correctly callback with selected build tool', () => {
  const setBuildTool = jest.fn();
  const wrapper = shallowRender({ setBuildTool });
  selectBuildTool(wrapper, BuildTools.Maven);

  expect(setBuildTool).toHaveBeenCalledWith(BuildTools.Maven);
});

function selectBuildTool(wrapper: ShallowWrapper<ProjectKeyStepProps>, tool: BuildTools) {
  const content = new ShallowWrapper(renderStepContent(wrapper) as JSX.Element);
  content.find(RenderOptions).props().onCheck(tool);
}

function shallowRender(props: Partial<ProjectKeyStepProps> = {}) {
  return shallow<ProjectKeyStepProps>(
    <ProjectKeyStep
      component={mockComponent()}
      finished={false}
      hasCLanguageFeature={true}
      onDone={jest.fn()}
      onOpen={jest.fn()}
      open={true}
      setBuildTool={jest.fn()}
      {...props}
    />
  );
}
