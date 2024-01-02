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
  mockAlmSettingsInstance,
  mockProjectBitbucketCloudBindingResponse,
} from '../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import Step from '../../components/Step';
import { renderStepContent } from '../../test-utils';
import BitbucketPipelinesTutorial, {
  BitbucketPipelinesTutorialProps,
} from '../BitbucketPipelinesTutorial';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ almBinding: undefined })).toMatchSnapshot('no binding');
  expect(renderStepContent(shallowRender())).toMatchSnapshot('repo variable step content');
  expect(renderStepContent(shallowRender(), 1)).toMatchSnapshot('yaml file step content');
});

it('should correctly navigate through the steps', () => {
  const wrapper = shallowRender();

  expect(wrapper.find(Step).at(0).props().open).toBe(true);
  expect(wrapper.find(Step).at(1).props().open).toBe(false);

  wrapper.find(Step).at(1).simulate('open');
  expect(wrapper.find(Step).at(0).props().open).toBe(false);
  expect(wrapper.find(Step).at(1).props().open).toBe(true);
});

function shallowRender(props: Partial<BitbucketPipelinesTutorialProps> = {}) {
  return shallow<BitbucketPipelinesTutorialProps>(
    <BitbucketPipelinesTutorial
      almBinding={mockAlmSettingsInstance()}
      baseUrl="test"
      mainBranchName="main"
      currentUser={mockLoggedInUser()}
      component={mockComponent()}
      projectBinding={mockProjectBitbucketCloudBindingResponse()}
      willRefreshAutomatically={true}
      {...props}
    />
  );
}
