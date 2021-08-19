/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { getChildren } from '../../../../api/components';
import { getMeasures } from '../../../../api/measures';
import handleRequiredAuthentication from '../../../../helpers/handleRequiredAuthentication';
import {
  mockComponent,
  mockCurrentUser,
  mockLocation,
  mockLoggedInUser,
  mockRouter
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { App } from '../App';
import UnsubscribeEmailModal from '../UnsubscribeEmailModal';

jest.mock('../../../../helpers/handleRequiredAuthentication', () => ({
  default: jest.fn()
}));

jest.mock('../../../../api/measures', () => ({
  getMeasures: jest.fn().mockResolvedValue([])
}));

jest.mock('../../../../api/components', () => ({
  getChildren: jest.fn().mockResolvedValue({ components: [], paging: { total: 0 } })
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  const wrapper = shallowRender({
    component: mockComponent({
      key: 'foo',
      name: 'Foo',
      qualifier: ComponentQualifier.Portfolio,
      description: 'accurate description'
    })
  });
  expect(wrapper).toMatchSnapshot('loading');

  wrapper.setState({ loading: false, measures: { reliability_rating: '1' } });
  expect(wrapper).toMatchSnapshot('portfolio is empty');

  wrapper.setState({ measures: { ncloc: '173' } });
  expect(wrapper).toMatchSnapshot('portfolio is not computed');

  wrapper.setState({
    measures: { ncloc: '173', reliability_rating: '1' },
    subComponents: [],
    totalSubComponents: 0
  });
  expect(wrapper).toMatchSnapshot('default');
});

it('should require authentication if this is an unsubscription request and user is anonymous', () => {
  shallowRender({ location: mockLocation({ query: { unsubscribe: '1' } }) });
  expect(handleRequiredAuthentication).toBeCalled();
});

it('should show the unsubscribe modal if this is an unsubscription request and user is logged in', async () => {
  (getMeasures as jest.Mock).mockResolvedValueOnce([
    { metric: 'ncloc', value: '173' },
    { metric: 'reliability_rating', value: '1' }
  ]);
  const wrapper = shallowRender({
    location: mockLocation({ query: { unsubscribe: '1' } }),
    currentUser: mockLoggedInUser()
  });

  await waitAndUpdate(wrapper);

  expect(handleRequiredAuthentication).not.toBeCalled();
  expect(wrapper.find(UnsubscribeEmailModal).exists()).toBe(true);
});

it('should update the location when unsubscribe modal is closed', () => {
  const replace = jest.fn();
  const wrapper = shallowRender({
    location: mockLocation({ query: { unsubscribe: '1' } }),
    currentUser: mockLoggedInUser(),
    router: mockRouter({ replace })
  });
  wrapper.instance().handleCloseUnsubscribeEmailModal();
  expect(replace).toBeCalledWith(expect.objectContaining({ query: { unsubscribe: undefined } }));
});

it('fetches measures and children components', () => {
  shallowRender();

  expect(getMeasures).toBeCalledWith({
    component: 'foo',
    metricKeys:
      'projects,ncloc,ncloc_language_distribution,releasability_rating,releasability_effort,sqale_rating,maintainability_rating_effort,reliability_rating,reliability_rating_effort,security_rating,security_rating_effort,security_review_rating,security_review_rating_effort,last_change_on_releasability_rating,last_change_on_maintainability_rating,last_change_on_security_rating,last_change_on_security_review_rating,last_change_on_reliability_rating'
  });

  expect(getChildren).toBeCalledWith(
    'foo',
    [
      'ncloc',
      'releasability_rating',
      'security_rating',
      'security_review_rating',
      'reliability_rating',
      'sqale_rating',
      'alert_status'
    ],
    { ps: 20, s: 'qualifier' }
  );
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      component={mockComponent({
        key: 'foo',
        name: 'Foo',
        qualifier: ComponentQualifier.Portfolio
      })}
      currentUser={mockCurrentUser()}
      fetchMetrics={jest.fn()}
      location={mockLocation()}
      metrics={{}}
      router={mockRouter()}
      {...props}
    />
  );
}
