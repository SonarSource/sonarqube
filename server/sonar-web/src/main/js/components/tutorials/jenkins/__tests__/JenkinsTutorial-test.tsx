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
import { mockProjectBitbucketBindingResponse } from '../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../helpers/mocks/component';
import { AlmKeys } from '../../../../types/alm-settings';
import JenkinsfileStep from '../JenkinsfileStep';
import { JenkinsTutorial, JenkinsTutorialProps } from '../JenkinsTutorial';
import MultiBranchPipelineStep from '../MultiBranchPipelineStep';
import PreRequisitesStep from '../PreRequisitesStep';
import SelectAlmStep from '../SelectAlmStep';
import WebhookStep from '../WebhookStep';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ hasFeature: () => false })).toMatchSnapshot('branches not enabled');
  expect(shallowRender({ projectBinding: undefined })).toMatchSnapshot('no project binding');
});

it('should correctly navigate between steps', () => {
  const wrapper = shallowRender();

  expect(wrapper.find(PreRequisitesStep).props().open).toBe(true);
  expect(wrapper.find(MultiBranchPipelineStep).props().open).toBe(false);
  expect(wrapper.find(WebhookStep).props().open).toBe(false);
  expect(wrapper.find(JenkinsfileStep).props().open).toBe(false);

  // Pre-reqs done.
  wrapper.find(PreRequisitesStep).props().onDone();
  expect(wrapper.find(PreRequisitesStep).props().open).toBe(false);
  expect(wrapper.find(MultiBranchPipelineStep).props().open).toBe(true);
  expect(wrapper.find(WebhookStep).props().open).toBe(false);
  expect(wrapper.find(JenkinsfileStep).props().open).toBe(false);

  // Multibranch done.
  wrapper.find(MultiBranchPipelineStep).props().onDone();
  expect(wrapper.find(PreRequisitesStep).props().open).toBe(false);
  expect(wrapper.find(MultiBranchPipelineStep).props().open).toBe(false);
  expect(wrapper.find(WebhookStep).props().open).toBe(true);
  expect(wrapper.find(JenkinsfileStep).props().open).toBe(false);

  // Webhook done.
  wrapper.find(WebhookStep).props().onDone();
  expect(wrapper.find(PreRequisitesStep).props().open).toBe(false);
  expect(wrapper.find(MultiBranchPipelineStep).props().open).toBe(false);
  expect(wrapper.find(WebhookStep).props().open).toBe(false);
  expect(wrapper.find(JenkinsfileStep).props().open).toBe(true);

  // Open Pre-reqs.
  wrapper.find(PreRequisitesStep).props().onOpen();
  expect(wrapper.find(PreRequisitesStep).props().open).toBe(true);

  // Open Multibranch.
  wrapper.find(MultiBranchPipelineStep).props().onOpen();
  expect(wrapper.find(MultiBranchPipelineStep).props().open).toBe(true);

  // Open Webhook.
  wrapper.find(WebhookStep).props().onOpen();
  expect(wrapper.find(WebhookStep).props().open).toBe(true);
});

it('should correctly select an ALM if no project is bound', () => {
  const wrapper = shallowRender({ projectBinding: undefined });
  expect(wrapper.find(PreRequisitesStep).exists()).toBe(false);
  wrapper.find(SelectAlmStep).props().onCheck(AlmKeys.BitbucketCloud);
  expect(wrapper.find(SelectAlmStep).props().open).toBe(false);
  expect(wrapper.find(PreRequisitesStep).exists()).toBe(true);
});

function shallowRender(props: Partial<JenkinsTutorialProps> = {}) {
  return shallow<JenkinsTutorialProps>(
    <JenkinsTutorial
      baseUrl=""
      hasFeature={jest.fn().mockReturnValue(true)}
      component={mockComponent()}
      projectBinding={mockProjectBitbucketBindingResponse()}
      willRefreshAutomatically={true}
      {...props}
    />
  );
}
