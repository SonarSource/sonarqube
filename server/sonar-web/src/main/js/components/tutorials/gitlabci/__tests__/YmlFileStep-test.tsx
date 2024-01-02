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
import { renderStepContent } from '../../test-utils';
import { BuildTools } from '../../types';
import { YmlFileStep, YmlFileStepProps } from '../YmlFileStep';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('Step wrapper');
  expect(renderStepContent(wrapper)).toMatchSnapshot('initial content');
});

it.each([
  [BuildTools.Maven],
  [BuildTools.Gradle],
  [BuildTools.DotNet],
  [BuildTools.CFamily],
  [BuildTools.Other],
])('should render correctly for build tool %s', (buildTool) => {
  expect(renderStepContent(shallowRender({ buildTool }))).toMatchSnapshot('with branch support');
  expect(renderStepContent(shallowRender({ hasFeature: () => false, buildTool }))).toMatchSnapshot(
    'without branch support'
  );
});

function shallowRender(props: Partial<YmlFileStepProps> = {}) {
  return shallow<YmlFileStepProps>(
    <YmlFileStep
      hasFeature={jest.fn().mockReturnValue(true)}
      open={true}
      projectKey="test"
      finished={true}
      mainBranchName="main"
      onDone={jest.fn()}
      onOpen={jest.fn()}
      {...props}
    />
  );
}
