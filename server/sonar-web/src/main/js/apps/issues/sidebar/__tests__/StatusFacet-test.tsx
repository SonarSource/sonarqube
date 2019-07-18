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
import { click } from 'sonar-ui-common/helpers/testUtils';
import StatusFacet from '../StatusFacet';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should toggle status facet', () => {
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onToggle });
  click(wrapper.children('FacetHeader'));
  expect(onToggle).toBeCalledWith('statuses');
});

it('should clear status facet', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange, statuses: ['TO_REVIEW'] });
  wrapper.children('FacetHeader').prop<Function>('onClear')();
  expect(onChange).toBeCalledWith({ statuses: [] });
});

it('should select a status', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange });
  clickAndCheck('TO_REVIEW');
  clickAndCheck('OPEN', true, ['OPEN', 'TO_REVIEW']);
  clickAndCheck('CONFIRMED');

  function clickAndCheck(status: string, multiple = false, expected = [status]) {
    wrapper
      .find(`FacetItemsList`)
      .find(`FacetItem[value="${status}"]`)
      .prop<Function>('onClick')(status, multiple);
    expect(onChange).lastCalledWith({ statuses: expected });
    wrapper.setProps({ statuses: expected });
  }
});

function shallowRender(props: Partial<StatusFacet['props']> = {}) {
  return shallow(
    <StatusFacet
      fetching={false}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={true}
      stats={{
        OPEN: 104,
        CONFIRMED: 8,
        REOPENED: 0,
        RESOLVED: 0,
        CLOSED: 8,
        TO_REVIEW: 150,
        IN_REVIEW: 7,
        REVIEWED: 1105
      }}
      statuses={[]}
      {...props}
    />
  );
}
