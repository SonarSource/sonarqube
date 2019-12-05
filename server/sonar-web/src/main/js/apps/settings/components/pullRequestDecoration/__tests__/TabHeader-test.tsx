/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { ALM_KEYS } from '../../../../../types/alm-settings';
import { TabHeader, TabHeaderProps } from '../TabHeader';

it('should render correctly', () => {
  expect(shallowRender(ALM_KEYS.AZURE)).toMatchSnapshot();
  expect(shallowRender(ALM_KEYS.GITHUB)).toMatchSnapshot();
});

it('should only show the create button if certain conditions are met', () => {
  expect(
    shallowRender(ALM_KEYS.GITHUB, { appState: { multipleAlmEnabled: false }, definitionCount: 1 })
      .find(Button)
      .exists()
  ).toBe(false);

  expect(
    shallowRender(ALM_KEYS.GITHUB, { appState: { multipleAlmEnabled: false }, definitionCount: 0 })
      .find(Button)
      .exists()
  ).toBe(true);

  expect(
    shallowRender(ALM_KEYS.GITHUB, { appState: { multipleAlmEnabled: true }, definitionCount: 5 })
      .find(Button)
      .exists()
  ).toBe(true);
});

function shallowRender(alm: ALM_KEYS, props: Partial<TabHeaderProps> = {}) {
  return shallow(
    <TabHeader alm={alm} appState={{}} definitionCount={0} onCreate={jest.fn()} {...props} />
  );
}
