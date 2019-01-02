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
import CardPlan, { FreeCardPlan, PaidCardPlan } from '../CardPlan';
import { click } from '../../../../helpers/testUtils';

it('should render correctly', () => {
  expect(
    shallow(
      <CardPlan recommended="Recommended for you" startingPrice={10} title="Paid Plan">
        <div>content</div>
      </CardPlan>
    )
  ).toMatchSnapshot();
});

it('should be actionable', () => {
  const onClick = jest.fn();
  const wrapper = shallow(
    <CardPlan onClick={onClick} title="Free Plan">
      <div>content</div>
    </CardPlan>
  );

  expect(wrapper).toMatchSnapshot();
  click(wrapper);
  wrapper.setProps({ selected: true, startingPrice: 0 });
  expect(wrapper).toMatchSnapshot();
});

describe('#FreeCardPlan', () => {
  it('should render', () => {
    expect(shallow(<FreeCardPlan hasWarning={false} />)).toMatchSnapshot();
  });

  it('should render with warning', () => {
    expect(
      shallow(<FreeCardPlan almName="GitHub" hasWarning={true} selected={true} />)
    ).toMatchSnapshot();
  });

  it('should render disabled with info', () => {
    expect(
      shallow(<FreeCardPlan almName="GitHub" disabled={true} hasWarning={false} />)
    ).toMatchSnapshot();
  });
});

describe('#PaidCardPlan', () => {
  it('should render', () => {
    expect(shallow(<PaidCardPlan isRecommended={true} startingPrice={10} />)).toMatchSnapshot();
  });
});
