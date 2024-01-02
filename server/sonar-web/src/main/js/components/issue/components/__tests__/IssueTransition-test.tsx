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
import IssueTransition from '../IssueTransition';

const issue: IssueTransition['props']['issue'] = {
  key: 'foo1234',
  transitions: ['confirm', 'resolve', 'falsepositive', 'wontfix'],
  status: 'OPEN',
  type: 'BUG',
};

it('should render without the action when there is no transitions', () => {
  expect(
    shallowRender({
      hasTransitions: false,
      issue: { key: 'foo1234', transitions: [], status: 'CLOSED', type: 'BUG' },
    })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<IssueTransition['props']> = {}) {
  return shallow(
    <IssueTransition
      hasTransitions={true}
      isOpen={false}
      issue={issue}
      onChange={jest.fn()}
      togglePopup={jest.fn()}
      {...props}
    />
  );
}
