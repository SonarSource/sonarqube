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
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { IssueCharacteristic } from '../../../../types/issues';
import IssueCharacteristicHeader, {
  IssueCharacteristicHeaderProps,
} from '../IssueCharacteristicHeader';

const ui = {
  characteristicLabel: (characteristic: IssueCharacteristic) =>
    byText(`issue.characteristic.${characteristic}`),
  docLink: byRole('link', { name: /issue.characteristic.doc.link/ }),
};

it('should render correctly', async () => {
  renderIssueCharacteristicHeader();

  expect(await ui.characteristicLabel(IssueCharacteristic.Clear).find()).toBeInTheDocument();
});

it('can select a link in tooltip using tab', async () => {
  renderIssueCharacteristicHeader();

  await userEvent.tab();
  expect(ui.characteristicLabel(IssueCharacteristic.Clear).get()).toHaveFocus();

  // Tooltip ignores any keyboard event if it is not Tab
  await userEvent.keyboard('A');

  await userEvent.tab();
  expect(ui.docLink.get()).toHaveFocus();

  await userEvent.tab();
  expect(ui.characteristicLabel(IssueCharacteristic.Clear).get()).toHaveFocus();
  expect(ui.docLink.query()).not.toBeInTheDocument();

  await userEvent.tab({ shift: true });
  await userEvent.tab();

  expect(ui.docLink.get()).toBeInTheDocument();
  await userEvent.tab({ shift: true });
  expect(ui.characteristicLabel(IssueCharacteristic.Clear).get()).not.toHaveFocus();
});

function renderIssueCharacteristicHeader(props: Partial<IssueCharacteristicHeaderProps> = {}) {
  return renderComponent(
    <IssueCharacteristicHeader characteristic={IssueCharacteristic.Clear} {...props} />
  );
}
