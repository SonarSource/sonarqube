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
import userEvent from '@testing-library/user-event';
import React from 'react';
import { Route } from 'react-router-dom';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { MessageTypes } from '../../../api/messages';
import MessagesServiceMock from '../../../api/mocks/MessagesServiceMock';
import NewCodeDefinitionServiceMock from '../../../api/mocks/NewCodeDefinitionServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';
import { Component } from '../../../types/types';
import NCDAutoUpdateMessage from '../NCDAutoUpdateMessage';

let newCodeDefinitionMock: NewCodeDefinitionServiceMock;
let messagesMock: MessagesServiceMock;

describe('Global NCD update notification banner', () => {
  function renderGlobalMessage(
    currentUser = mockLoggedInUser({ permissions: { global: ['admin'] } }),
  ) {
    return renderAppRoutes(
      '/',
      () => (
        <>
          <Route path="/" element={<NCDAutoUpdateMessage />} />
          <Route path="/admin/settings" element={<div>Admin NCD</div>} />
        </>
      ),
      {
        currentUser,
      },
    );
  }

  beforeAll(() => {
    newCodeDefinitionMock = new NewCodeDefinitionServiceMock();
    messagesMock = new MessagesServiceMock();
  });

  afterEach(() => {
    newCodeDefinitionMock.reset();
    messagesMock.reset();
  });

  const ui = {
    dismissButton: byRole('button', { name: 'dismiss' }),
    globalBannerContent: byText(/new_code_definition.auto_update.global.message/),
    reviewLink: byText('new_code_definition.auto_update.review_link'),
    adminNcdMessage: byText('Admin NCD'),
  };
  const previouslyNonCompliantNewCodeDefinition = {
    previousNonCompliantValue: '120',
    type: NewCodeDefinitionType.NumberOfDays,
    updatedAt: 1692106874855,
    value: '90',
  };

  it('renders no global banner if user is not global admin', () => {
    const { container } = renderGlobalMessage(mockLoggedInUser());
    expect(container).toContainHTML('<div><div class="Toastify" /></div>');
  });

  it('renders global banner if user is global admin', async () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    renderGlobalMessage();
    expect(await ui.globalBannerContent.find()).toBeVisible();
  });

  it('dismisses global banner', async () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    renderGlobalMessage();
    expect(await ui.globalBannerContent.find()).toBeVisible();
    const user = userEvent.setup();
    await user.click(ui.dismissButton.get());
    expect(ui.globalBannerContent.query()).not.toBeInTheDocument();
  });

  it('does not render global banner if dismissed', () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    messagesMock.setMessageDismissed({ messageType: MessageTypes.GlobalNcd90 });
    renderGlobalMessage();
    expect(ui.globalBannerContent.query()).not.toBeInTheDocument();
  });

  it('does not render global banner if global new code definition has not been automatically updated', () => {
    newCodeDefinitionMock.setNewCodePeriod({
      type: NewCodeDefinitionType.NumberOfDays,
      value: '45',
    });
    renderGlobalMessage();
    expect(ui.globalBannerContent.query()).not.toBeInTheDocument();
  });

  it('clicking on review link redirects to global NCD admin page', async () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    renderGlobalMessage();
    expect(await ui.globalBannerContent.find()).toBeVisible();
    const user = userEvent.setup();
    await user.click(ui.reviewLink.get());
    expect(await ui.adminNcdMessage.find()).toBeVisible();
  });
});

describe('Project NCD update notification banner', () => {
  function renderProjectMessage(component: Component, currentUser = mockLoggedInUser()) {
    return renderAppRoutes(
      '/',
      () => (
        <>
          <Route path="/" element={<NCDAutoUpdateMessage component={component} />} />
          <Route path="/project/baseline" element={<div>Project NCD</div>} />
        </>
      ),
      {
        currentUser,
      },
    );
  }

  beforeAll(() => {
    newCodeDefinitionMock = new NewCodeDefinitionServiceMock();
    messagesMock = new MessagesServiceMock();
  });

  afterEach(() => {
    newCodeDefinitionMock.reset();
    messagesMock.reset();
  });

  const ui = {
    dismissButton: byRole('button', { name: 'dismiss' }),
    projectBannerContent: byText(/new_code_definition.auto_update.project.message/),
    projectNcdMessage: byText('Project NCD'),
    reviewLink: byText('new_code_definition.auto_update.review_link'),
  };

  const component = mockComponent({
    key: 'test-project:test',
    configuration: { showSettings: true },
  });
  const previouslyNonCompliantNewCodeDefinition = {
    previousNonCompliantValue: '120',
    projectKey: component.key,
    type: NewCodeDefinitionType.NumberOfDays,
    updatedAt: 1692106874855,
    value: '90',
  };

  it('renders no project banner if user is not project admin', () => {
    const { container } = renderProjectMessage(
      mockComponent({ configuration: { showSettings: false } }),
    );
    expect(container).toContainHTML('<div><div class="Toastify" /></div>');
  });

  it('renders project banner if user is project admin', async () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    renderProjectMessage(component);
    expect(await ui.projectBannerContent.find()).toBeVisible();
  });

  it('dismisses project banner', async () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    renderProjectMessage(component);
    expect(await ui.projectBannerContent.find()).toBeVisible();
    const user = userEvent.setup();
    await user.click(ui.dismissButton.get());
    expect(ui.projectBannerContent.query()).not.toBeInTheDocument();
  });

  it('does not render project banner if dismissed', () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    messagesMock.setMessageDismissed({ messageType: MessageTypes.GlobalNcd90 });
    renderProjectMessage(component);
    expect(ui.projectBannerContent.query()).not.toBeInTheDocument();
  });

  it('does not render project banner if project new code definition has not been automatically updated', () => {
    newCodeDefinitionMock.setNewCodePeriod({
      projectKey: component.key,
      type: NewCodeDefinitionType.NumberOfDays,
      value: '45',
    });
    renderProjectMessage(component);
    expect(ui.projectBannerContent.query()).not.toBeInTheDocument();
  });

  it('clicking on review link redirects to project NCD admin page', async () => {
    newCodeDefinitionMock.setNewCodePeriod(previouslyNonCompliantNewCodeDefinition);
    renderProjectMessage(component);
    expect(await ui.projectBannerContent.find()).toBeVisible();
    const user = userEvent.setup();
    await user.click(ui.reviewLink.get());
    expect(await ui.projectNcdMessage.find()).toBeVisible();
  });
});
