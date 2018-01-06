/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import IssueTransition from '../IssueTransition';
import { click } from '../../../../helpers/testUtils';

const issue = {
  transitions: ['confirm', 'resolve', 'falsepositive', 'wontfix'],
  status: 'OPEN'
};

it('should render without the action when there is no transitions', () => {
  const element = shallow(
    <IssueTransition
      hasTransitions={false}
      isOpen={false}
      issue={{
        transitions: [],
        status: 'CLOSED'
      }}
      setIssueProperty={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render with the action', () => {
  const element = shallow(
    <IssueTransition
      hasTransitions={true}
      isOpen={false}
      issue={issue}
      setIssueProperty={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render with a resolution', () => {
  const element = shallow(
    <IssueTransition
      hasTransitions={true}
      isOpen={false}
      issue={{
        transitions: ['reopen'],
        status: 'RESOLVED',
        resolution: 'FIXED'
      }}
      setIssueProperty={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should open the popup when the button is clicked', () => {
  const toggle = jest.fn();
  const element = shallow(
    <IssueTransition
      hasTransitions={true}
      isOpen={false}
      issue={issue}
      setIssueProperty={jest.fn()}
      togglePopup={toggle}
    />
  );
  click(element.find('button'));
  expect(toggle.mock.calls).toMatchSnapshot();
  element.setProps({ isOpen: true });
  expect(element).toMatchSnapshot();
});
