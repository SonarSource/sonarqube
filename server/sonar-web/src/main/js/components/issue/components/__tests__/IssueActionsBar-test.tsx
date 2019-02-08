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
import * as React from 'react';
import { shallow } from 'enzyme';
import IssueActionsBar from '../IssueActionsBar';
import { mockIssue } from '../../../../helpers/testMocks';

jest.mock('../../actions', () => ({ updateIssue: jest.fn() }));

it('should render issue correctly', () => {
  const element = shallow(
    <IssueActionsBar
      issue={mockIssue()}
      onAssign={jest.fn()}
      onChange={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render security hotspot correctly', () => {
  const element = shallow(
    <IssueActionsBar
      issue={mockIssue(false, { type: 'SECURITY_HOTSPOT' })}
      onAssign={jest.fn()}
      onChange={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render commentable correctly', () => {
  const element = shallow(
    <IssueActionsBar
      issue={mockIssue(false, { actions: ['comment'] })}
      onAssign={jest.fn()}
      onChange={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render effort correctly', () => {
  const element = shallow(
    <IssueActionsBar
      issue={mockIssue(false, { effort: 'great' })}
      onAssign={jest.fn()}
      onChange={jest.fn()}
      togglePopup={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

describe('callback', () => {
  const issue: T.Issue = mockIssue();
  const onChangeMock = jest.fn();
  const togglePopupMock = jest.fn();

  const element = shallow<IssueActionsBar>(
    <IssueActionsBar
      issue={issue}
      onAssign={jest.fn()}
      onChange={onChangeMock}
      togglePopup={togglePopupMock}
    />
  );

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('handleTransition should call onChange', () => {
    const instance = element.instance();
    instance.handleTransition(issue);
    expect(onChangeMock).toHaveBeenCalledTimes(1);
    expect(togglePopupMock).toHaveBeenCalledTimes(0);
  });

  it('setIssueProperty should call togglePopup', () => {
    const instance = element.instance();

    const apiCallMock = jest.fn();

    instance.setIssueProperty('author', 'popup', apiCallMock, 'Jay');
    expect(togglePopupMock).toHaveBeenCalledTimes(1);
    expect(apiCallMock).toBeCalledTimes(1);
  });

  it('toggleComment should call togglePopup and update state', () => {
    const instance = element.instance();

    expect(element.state('commentAutoTriggered')).toBe(false);
    expect(element.state('commentPlaceholder')).toBe('');

    instance.toggleComment(false, 'hold my place', true);

    expect(element.state('commentAutoTriggered')).toBe(true);
    expect(element.state('commentPlaceholder')).toBe('hold my place');

    expect(togglePopupMock).toHaveBeenCalledTimes(1);
  });
});
