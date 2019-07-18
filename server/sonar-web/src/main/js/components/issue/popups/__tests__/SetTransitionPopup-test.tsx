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
import { hasMessage } from 'sonar-ui-common/helpers/l10n';
import SetTransitionPopup, { Props } from '../SetTransitionPopup';

jest.mock('sonar-ui-common/helpers/l10n', () => ({
  ...jest.requireActual('sonar-ui-common/helpers/l10n'),
  hasMessage: jest.fn().mockReturnValue(false)
}));

it('should render transition popup correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render transition popup correctly for vulnerability', () => {
  (hasMessage as jest.Mock).mockReturnValueOnce('true');
  expect(
    shallowRender({
      fromHotspot: true,
      transitions: ['resolveasreviewed', 'confirm']
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <SetTransitionPopup
      fromHotspot={false}
      onSelect={jest.fn()}
      transitions={['confirm', 'resolve', 'falsepositive', 'wontfix']}
      type="VULNERABILITY"
      {...props}
    />
  );
}
