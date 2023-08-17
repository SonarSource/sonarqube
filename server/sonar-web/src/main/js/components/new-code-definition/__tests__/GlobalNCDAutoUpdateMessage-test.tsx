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
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { Route } from 'react-router-dom';
import { MessageTypes } from '../../../api/messages';
import MessagesServiceMock from '../../../api/mocks/MessagesServiceMock';
import NewCodePeriodsServiceMock from '../../../api/mocks/NewCodePeriodsServiceMock';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import { NewCodeDefinitionType } from '../../../types/new-code-definition';
import { GlobalNCDAutoUpdateMessage } from '../GlobalNCDAutoUpdateMessage';

let newCodeDefinitionMock: NewCodePeriodsServiceMock;
let messagesMock: MessagesServiceMock;

beforeAll(() => {
  newCodeDefinitionMock = new NewCodePeriodsServiceMock();
  messagesMock = new MessagesServiceMock();
});

afterEach(() => {
  newCodeDefinitionMock.reset();
  messagesMock.reset();
});

const ui = {
  message: byText(/new_code_definition.auto_update.message/),
  dismissButton: byRole('button', { name: 'dismiss' }),
  reviewLink: byText('new_code_definition.auto_update.review_link'),
  adminNcdMessage: byText('Admin NCD'),
};

it('renders nothing if user is not admin', () => {
  const { container } = renderMessage(mockLoggedInUser());
  expect(container).toBeEmptyDOMElement();
});

it('renders message if user is admin', async () => {
  newCodeDefinitionMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '90',
    previousNonCompliantValue: '120',
    updatedAt: 1692106874855,
  });
  renderMessage();
  expect(await ui.message.find()).toBeVisible();
});

it('dismisses message', async () => {
  newCodeDefinitionMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '90',
    previousNonCompliantValue: '120',
    updatedAt: 1692106874855,
  });
  renderMessage();
  expect(await ui.message.find()).toBeVisible();
  const user = userEvent.setup();
  await act(async () => {
    await user.click(ui.dismissButton.get());
  });
  expect(ui.message.query()).not.toBeInTheDocument();
});

it('does not render message if dismissed', () => {
  newCodeDefinitionMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '90',
    previousNonCompliantValue: '120',
    updatedAt: 1692106874855,
  });
  messagesMock.setMessageDismissed({ messageType: MessageTypes.GlobalNcd90 });
  renderMessage();
  expect(ui.message.query()).not.toBeInTheDocument();
});

it('does not render message if new code definition has not been automatically updated', () => {
  newCodeDefinitionMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '45',
  });
  renderMessage();
  expect(ui.message.query()).not.toBeInTheDocument();
});

it('clicking on review link redirects to NCD admin page', async () => {
  newCodeDefinitionMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '90',
    previousNonCompliantValue: '120',
    updatedAt: 1692106874855,
  });
  renderMessage();
  expect(await ui.message.find()).toBeVisible();
  const user = userEvent.setup();
  await act(async () => {
    await user.click(ui.reviewLink.get());
  });
  expect(await ui.adminNcdMessage.find()).toBeVisible();
});

function renderMessage(currentUser = mockLoggedInUser({ permissions: { global: ['admin'] } })) {
  return renderAppRoutes('/', () => (
    <>
      <Route path="/" element={<GlobalNCDAutoUpdateMessage currentUser={currentUser} />} />
      <Route path="/admin/settings" element={<div>Admin NCD</div>} />
    </>
  ));
}
