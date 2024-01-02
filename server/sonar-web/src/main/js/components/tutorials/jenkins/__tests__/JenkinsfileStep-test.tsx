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
import Step from '../../components/Step';
import { renderStepContent } from '../../test-utils';
import { BuildTools } from '../../types';
import { JenkinsfileStep, JenkinsfileStepProps } from '../JenkinsfileStep';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('Step wrapper');
  wrapper.setProps({ hasCLanguageFeature: true });
  expect(wrapper).toMatchSnapshot('Step wrapper with C');
  expect(renderStepContent(wrapper)).toMatchSnapshot('initial content');
});

it('should render correctly for Maven', () => {
  const wrapper = shallowRender();
  selectBuildTool(wrapper, BuildTools.Maven);
  expect(wrapper.find(Step).props().renderForm()).toMatchSnapshot();
});

it('should render correctly for Gradle', () => {
  const wrapper = shallowRender();
  selectBuildTool(wrapper, BuildTools.Gradle);
  expect(renderStepContent(wrapper)).toMatchSnapshot();
});

it('should render correctly for .NET', () => {
  const wrapper = shallowRender();
  selectBuildTool(wrapper, BuildTools.DotNet);
  expect(renderStepContent(wrapper)).toMatchSnapshot();
});

it('should render correctly for Other', () => {
  const wrapper = shallowRender();
  selectBuildTool(wrapper, BuildTools.Other);
  expect(renderStepContent(wrapper)).toMatchSnapshot();
});

function selectBuildTool(wrapper: ShallowWrapper<JenkinsfileStepProps>, tool: BuildTools) {
  const content = new ShallowWrapper(renderStepContent(wrapper) as JSX.Element);
  content.find(RenderOptions).prop('onCheck')(tool);
}

function shallowRender(props: Partial<JenkinsfileStepProps> = {}) {
  return shallow<JenkinsfileStepProps>(
    <JenkinsfileStep
      baseUrl="nice_url"
      component={mockComponent()}
      hasCLanguageFeature={false}
      finished={false}
      onDone={jest.fn()}
      onOpen={jest.fn()}
      open={true}
      {...props}
    />
  );
}
