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
import { mockAppState } from '../../../helpers/testMocks';
import { EditionKey } from '../../../types/editions';
import { GlobalFooter, GlobalFooterProps } from '../GlobalFooter';

it('should render the only logged in information', () => {
  expect(getWrapper()).toMatchSnapshot();
});

it('should not render the only logged in information', () => {
  expect(
    getWrapper({
      hideLoggedInInfo: true,
      appState: mockAppState({ version: '6.4-SNAPSHOT' }),
    })
  ).toMatchSnapshot();
});

it('should show the db warning message', () => {
  expect(
    getWrapper({
      appState: mockAppState({ productionDatabase: false, edition: EditionKey.community }),
    }).find('Alert')
  ).toMatchSnapshot();
});

it('should display the sq version', () => {
  expect(
    getWrapper({
      appState: mockAppState({ edition: EditionKey.enterprise, version: '6.4-SNAPSHOT' }),
    })
  ).toMatchSnapshot();
});

function getWrapper(props?: GlobalFooterProps) {
  return shallow(
    <GlobalFooter
      appState={mockAppState({ productionDatabase: true, edition: EditionKey.community })}
      {...props}
    />
  );
}
