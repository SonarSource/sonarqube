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
import { click } from 'sonar-ui-common/helpers/testUtils';
import {
  mockBitbucketBindingDefinition,
  mockProjectBitbucketBindingResponse,
  mockProjectGitLabBindingResponse
} from '../../../helpers/mocks/alm-settings';
import { mockComponent, mockLoggedInUser } from '../../../helpers/testMocks';
import TutorialSelectionRenderer, {
  TutorialSelectionRendererProps
} from '../TutorialSelectionRenderer';
import { TutorialModes } from '../types';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('selection');
  expect(shallowRender({ projectBinding: mockProjectBitbucketBindingResponse() })).toMatchSnapshot(
    'selection with jenkins available'
  );
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ selectedTutorial: TutorialModes.Manual })).toMatchSnapshot(
    'manual tutorial'
  );
  expect(
    shallowRender({
      selectedTutorial: TutorialModes.Jenkins,
      projectBinding: mockProjectBitbucketBindingResponse()
    })
  ).toMatchSnapshot('jenkins tutorial');
  expect(
    shallowRender({
      selectedTutorial: TutorialModes.GitLabCI,
      projectBinding: mockProjectGitLabBindingResponse()
    })
  ).toMatchSnapshot('gitlab tutorial');
});

it('should allow mode selection', () => {
  const onSelectTutorial = jest.fn();
  const wrapper = shallowRender({
    onSelectTutorial,
    projectBinding: mockProjectBitbucketBindingResponse()
  });

  click(wrapper.find('button.tutorial-mode-jenkins'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Jenkins);

  click(wrapper.find('button.tutorial-mode-manual'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Manual);
});

it('should allow gitlab selection', () => {
  const onSelectTutorial = jest.fn();
  const wrapper = shallowRender({
    onSelectTutorial,
    projectBinding: mockProjectGitLabBindingResponse()
  });

  click(wrapper.find('button.tutorial-mode-gitlab'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.GitLabCI);
});

function shallowRender(props: Partial<TutorialSelectionRendererProps> = {}) {
  return shallow<TutorialSelectionRendererProps>(
    <TutorialSelectionRenderer
      almBinding={mockBitbucketBindingDefinition()}
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      loading={false}
      onSelectTutorial={jest.fn()}
      {...props}
    />
  );
}
