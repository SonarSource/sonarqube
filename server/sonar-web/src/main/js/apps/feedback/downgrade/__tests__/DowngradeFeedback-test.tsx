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
import DowngradeFeedback, { LocationState } from '../DowngradeFeedback';
import { giveDowngradeFeedback } from '../../../../api/billing';
import { mockLocation, mockRouter } from '../../../../helpers/testMocks';

jest.mock('../../../../api/billing', () => ({
  giveDowngradeFeedback: jest.fn()
}));

const mockRouterReplace = jest.fn();

const org: T.Organization = {
  key: 'myorg',
  name: 'My Org'
};

const returnPath = '/path?with=query';

beforeEach(() => {
  (mockRouterReplace as jest.Mock).mockClear();
  (giveDowngradeFeedback as jest.Mock).mockClear();
});

it('should render correctly', () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();
});

it('should enforce choosing a reason, and show an extra textarea if a reason was chosen', () => {
  const wrapper = getWrapper();
  expect(wrapper.find('SubmitButton')).toMatchSnapshot();

  wrapper.setState({ feedback: 'other' });
  expect(wrapper.find('[name="reason_text_other"]').exists()).toBe(true);
  expect(wrapper.find('SubmitButton')).toMatchSnapshot();
});

it('should submit the data to the webservice', () => {
  const wrapper = getWrapper();
  const feedback = 'other';
  const additionalFeedback = 'Additional feedback';
  wrapper.setState({ feedback, additionalFeedback });
  wrapper
    .find('form')
    .at(0)
    .simulate('submit', {
      preventDefault: jest.fn()
    });
  expect(giveDowngradeFeedback).toBeCalledWith({
    organization: org.key,
    feedback,
    additionalFeedback
  });
  expect(mockRouterReplace).toBeCalledWith({
    pathname: returnPath
  });
});

function mockLocationState(overrides = {}): LocationState {
  return {
    confirmationMessage: 'Downgrade successful',
    returnTo: returnPath,
    organization: org,
    title: 'Title',
    ...overrides
  };
}

function getWrapper(props = {}, locationState = {}) {
  return shallow(
    <DowngradeFeedback
      location={mockLocation({
        state: mockLocationState(locationState)
      })}
      params={{}}
      router={mockRouter({
        replace: mockRouterReplace
      })}
      routes={[]}
      {...props}
    />
  );
}
