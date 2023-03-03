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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byText } from 'testing-library-selector';
import NewCodePeriodsServiceMock from '../../../../api/mocks/NewCodePeriodsServiceMock';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import NewCodePeriod from '../NewCodePeriod';

let newCodeMock: NewCodePeriodsServiceMock;

beforeAll(() => {
  newCodeMock = new NewCodePeriodsServiceMock();
});

afterEach(() => {
  newCodeMock.reset();
});

const ui = {
  newCodeTitle: byRole('heading', { name: 'settings.new_code_period.title' }),
  savedMsg: byText('settings.state.saved'),
  prevVersionRadio: byRole('radio', { name: /baseline.previous_version/ }),
  daysNumberRadio: byRole('radio', { name: /baseline.number_days/ }),
  daysInput: byRole('textbox'),
  saveButton: byRole('button', { name: 'save' }),
  cancelButton: byRole('button', { name: 'cancel' }),
};

it('renders and behaves as expected', async () => {
  const user = userEvent.setup();
  renderNewCodePeriod();

  expect(await ui.newCodeTitle.find()).toBeInTheDocument();
  // Previous version should be checked by default
  expect(ui.prevVersionRadio.get()).toBeChecked();

  // Can select number of days
  await user.click(ui.daysNumberRadio.get());
  expect(ui.daysNumberRadio.get()).toBeChecked();

  // Save should be disabled for zero or NaN
  expect(ui.daysInput.get()).toHaveValue('30');
  await user.clear(ui.daysInput.get());
  await user.type(ui.daysInput.get(), '0');
  expect(await ui.saveButton.find()).toBeDisabled();
  await user.clear(ui.daysInput.get());
  await user.type(ui.daysInput.get(), 'asdas');
  expect(ui.saveButton.get()).toBeDisabled();
  await user.clear(ui.daysInput.get());

  // Save enabled for valid days number
  await user.type(ui.daysInput.get(), '10');
  expect(ui.saveButton.get()).toBeEnabled();

  // Can cancel action
  await user.click(ui.cancelButton.get());
  expect(ui.prevVersionRadio.get()).toBeChecked();

  // Can save change
  await user.click(ui.daysNumberRadio.get());
  await user.type(ui.daysInput.get(), '10');
  await user.click(ui.saveButton.get());
  expect(ui.savedMsg.get()).toBeInTheDocument();

  await user.click(ui.prevVersionRadio.get());
  await user.click(ui.cancelButton.get());
  await user.click(ui.prevVersionRadio.get());
  await user.click(ui.saveButton.get());
  expect(ui.savedMsg.get()).toBeInTheDocument();
});

function renderNewCodePeriod() {
  return renderComponent(<NewCodePeriod />);
}
