/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
  mockProjectAzureBindingResponse,
  mockProjectBitbucketBindingResponse,
  mockProjectBitbucketCloudBindingResponse,
  mockProjectGithubBindingResponse,
  mockProjectGitLabBindingResponse
} from '../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { click } from '../../../helpers/testUtils';
import TutorialSelectionRenderer, {
  TutorialSelectionRendererProps
} from '../TutorialSelectionRenderer';
import { TutorialModes } from '../types';

it.each([
  ['bitbucket server', mockProjectBitbucketBindingResponse()],
  ['github', mockProjectGithubBindingResponse()],
  ['gitlab', mockProjectGitLabBindingResponse()],
  ['azure', mockProjectAzureBindingResponse()]
])('should render correctly for %s', (_, projectBinding) => {
  expect(shallowRender({ projectBinding })).toMatchSnapshot();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('selection');
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
      selectedTutorial: TutorialModes.GitHubActions,
      projectBinding: mockProjectGithubBindingResponse()
    })
  ).toMatchSnapshot('github actions tutorial');
  expect(
    shallowRender({
      selectedTutorial: TutorialModes.GitLabCI,
      projectBinding: mockProjectGitLabBindingResponse()
    })
  ).toMatchSnapshot('gitlab tutorial');
  expect(
    shallowRender({
      selectedTutorial: TutorialModes.AzurePipelines,
      projectBinding: mockProjectAzureBindingResponse()
    })
  ).toMatchSnapshot('azure pipelines tutorial');
});

it('should allow mode selection for Bitbucket', () => {
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

it('should allow mode selection for Github', () => {
  const onSelectTutorial = jest.fn();
  const wrapper = shallowRender({
    onSelectTutorial,
    projectBinding: mockProjectGithubBindingResponse()
  });

  click(wrapper.find('button.tutorial-mode-jenkins'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Jenkins);

  click(wrapper.find('button.tutorial-mode-manual'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Manual);

  click(wrapper.find('button.tutorial-mode-github-actions'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.GitHubActions);

  click(wrapper.find('button.tutorial-mode-azure-pipelines'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.AzurePipelines);
});

it('should allow mode selection for GitLab', () => {
  const onSelectTutorial = jest.fn();
  const wrapper = shallowRender({
    onSelectTutorial,
    projectBinding: mockProjectGitLabBindingResponse()
  });

  click(wrapper.find('button.tutorial-mode-jenkins'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Jenkins);

  click(wrapper.find('button.tutorial-mode-gitlab-ci'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.GitLabCI);

  click(wrapper.find('button.tutorial-mode-manual'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Manual);
});

it('should allow mode selection for Bitbucket pipepline', () => {
  const onSelectTutorial = jest.fn();
  const wrapper = shallowRender({
    onSelectTutorial,
    projectBinding: mockProjectBitbucketCloudBindingResponse()
  });

  click(wrapper.find('button.tutorial-mode-jenkins'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Jenkins);

  click(wrapper.find('button.tutorial-mode-bitbucket-pipelines'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.BitbucketPipelines);

  click(wrapper.find('button.tutorial-mode-manual'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Manual);
});

it('should allow mode selection for Azure DevOps', () => {
  const onSelectTutorial = jest.fn();
  const wrapper = shallowRender({
    onSelectTutorial,
    projectBinding: mockProjectAzureBindingResponse()
  });

  click(wrapper.find('button.tutorial-mode-azure-pipelines'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.AzurePipelines);

  click(wrapper.find('button.tutorial-mode-manual'));
  expect(onSelectTutorial).toHaveBeenLastCalledWith(TutorialModes.Manual);
});

function shallowRender(props: Partial<TutorialSelectionRendererProps> = {}) {
  return shallow<TutorialSelectionRendererProps>(
    <TutorialSelectionRenderer
      almBinding={mockAlmSettingsInstance()}
      baseUrl="http://localhost:9000"
      component={mockComponent()}
      currentUser={mockLoggedInUser()}
      loading={false}
      onSelectTutorial={jest.fn()}
      {...props}
    />
  );
}
