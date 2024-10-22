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
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { MessageTypes } from '../../../../api/messages';
import MessagesServiceMock from '../../../../api/mocks/MessagesServiceMock';
import NewCodeDefinitionServiceMock from '../../../../api/mocks/NewCodeDefinitionServiceMock';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { NewCodeDefinitionType } from '../../../../types/new-code-definition';
import NewCodeDefinition from '../NewCodeDefinition';

let newCodeMock: NewCodeDefinitionServiceMock;
let messagesMock: MessagesServiceMock;

beforeAll(() => {
  newCodeMock = new NewCodeDefinitionServiceMock();
  messagesMock = new MessagesServiceMock();
});

afterEach(() => {
  newCodeMock.reset();
  messagesMock.reset();
});

const ui = {
  newCodeTitle: byRole('heading', { name: 'settings.new_code_period.title' }),
  savedMsg: byText('settings.state.saved'),
  prevVersionRadio: byRole('radio', { name: /new_code_definition.previous_version/ }),
  daysNumberRadio: byRole('radio', { name: /new_code_definition.number_days/ }),
  daysNumberErrorMessage: byText('new_code_definition.number_days.invalid', { exact: false }),
  daysInput: byRole('spinbutton') /* spinbutton is the default role for a number input */,
  saveButton: byRole('button', { name: 'save' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  ncdAutoUpdateMessage: byText(/new_code_definition.auto_update.ncd_page.message/),
  ncdAutoUpdateMessageDismiss: byRole('button', { name: 'dismiss' }),
};

it('renders and behaves as expected', async () => {
  const user = userEvent.setup();
  renderNewCodePeriod();

  expect(await ui.newCodeTitle.find()).toBeInTheDocument();
  // Previous version should be checked by default
  expect(await ui.prevVersionRadio.find()).toBeChecked();

  // Can select number of days
  await user.click(ui.daysNumberRadio.get());
  expect(ui.daysNumberRadio.get()).toBeChecked();

  // Save should be disabled for zero
  expect(ui.daysInput.get()).toHaveValue(30);
  await user.clear(ui.daysInput.get());
  await user.type(ui.daysInput.get(), '0');
  expect(await ui.saveButton.find()).toBeDisabled();

  // Save should not appear at all for NaN
  await user.clear(ui.daysInput.get());
  await user.type(ui.daysInput.get(), 'asdas');
  expect(ui.saveButton.query()).toBeDisabled();

  // Save enabled for valid days number
  await user.clear(ui.daysInput.get());
  await user.type(ui.daysInput.get(), '10');
  expect(ui.saveButton.get()).toBeEnabled();

  // Can cancel action
  await user.click(ui.cancelButton.get());
  expect(ui.prevVersionRadio.get()).toBeChecked();

  // Can save change
  await user.click(ui.daysNumberRadio.get());
  await user.clear(ui.daysInput.get());
  await user.type(ui.daysInput.get(), '10');
  await user.click(ui.saveButton.get());
  expect(ui.saveButton.get()).toBeDisabled();

  await user.click(ui.prevVersionRadio.get());
  await user.click(ui.cancelButton.get());
  await user.click(ui.prevVersionRadio.get());
  await user.click(ui.saveButton.get());
  expect(ui.saveButton.get()).toBeDisabled();
});

it('displays & dismisses information message when NCD is automatically updated', async () => {
  const assertError = spyOnLogError();

  newCodeMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '90',
    previousNonCompliantValue: '120',
    updatedAt: 1692279521904,
  });
  renderNewCodePeriod();

  expect(await ui.ncdAutoUpdateMessage.find()).toBeVisible();

  const user = userEvent.setup();
  await user.click(ui.ncdAutoUpdateMessageDismiss.get());

  expect(ui.ncdAutoUpdateMessage.query()).not.toBeInTheDocument();

  assertError();
});

it('does not display information message when NCD is automatically updated if message is already dismissed', () => {
  newCodeMock.setNewCodePeriod({
    type: NewCodeDefinitionType.NumberOfDays,
    value: '90',
    previousNonCompliantValue: '120',
    updatedAt: 1692279521904,
  });
  messagesMock.setMessageDismissed({ messageType: MessageTypes.GlobalNcdPage90 });
  renderNewCodePeriod();

  expect(ui.ncdAutoUpdateMessage.query()).not.toBeInTheDocument();
});

function spyOnLogError() {
  const errorSpy = jest.spyOn(console, 'error').mockImplementationOnce(() => {});

  return () => {
    // There is an issue with the design of NewCodeDefinitionDaysOption that causes a domNesting error
    // this is valid and has to be fixed, but this is not the purpose of this test
    expect(errorSpy).toHaveBeenCalledTimes(1);
    expect(errorSpy.mock.calls[0][0]).toBe(
      'Warning: validateDOMNesting(...): %s cannot appear as a descendant of <%s>.%s',
    );
    expect(errorSpy.mock.calls[0][1]).toBe('<button>');
    expect(errorSpy.mock.calls[0][2]).toBe('button');
    errorSpy.mockRestore();
  };
}

function renderNewCodePeriod() {
  return renderComponent(<NewCodeDefinition />);
}
