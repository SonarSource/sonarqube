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
import { mockAlmOrganization } from '../../../../helpers/testMocks';
import PlanSelect, { Plan } from '../PlanSelect';

it('should render and select', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange });
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('PaidCardPlan'));
  expect(onChange).toBeCalledWith(Plan.Paid);
  wrapper.setProps({ plan: Plan.Paid });
  expect(wrapper).toMatchSnapshot();
});

it('should recommend paid plan', () => {
  const wrapper = shallowRender({
    almOrganization: mockAlmOrganization({ privateRepos: 1, publicRepos: 5 }),
    plan: Plan.Paid
  });
  expect(wrapper.find('PaidCardPlan').prop('isRecommended')).toBe(true);
  expect(wrapper.find('FreeCardPlan').prop('disabled')).toBe(false);
  expect(wrapper.find('FreeCardPlan').prop('hasWarning')).toBe(false);

  wrapper.setProps({ plan: Plan.Free });
  expect(wrapper.find('FreeCardPlan').prop('hasWarning')).toBe(true);
});

it('should recommend paid plan and disable free plan', () => {
  const wrapper = shallowRender({
    almOrganization: mockAlmOrganization({ privateRepos: 1, publicRepos: 0 })
  });
  expect(wrapper.find('PaidCardPlan').prop('isRecommended')).toBe(true);
  expect(wrapper.find('FreeCardPlan').prop('disabled')).toBe(true);
});

function shallowRender(props: Partial<PlanSelect['props']> = {}) {
  return shallow(
    <PlanSelect onChange={jest.fn()} plan={Plan.Free} startingPrice={10} {...props} />
  );
}
