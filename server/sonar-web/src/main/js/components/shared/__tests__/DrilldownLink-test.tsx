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
import DrilldownLink from '../DrilldownLink';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});
it('should render issuesLink correctly', () => {
  const wrapper = shallowRender({ metric: 'new_violations' });
  expect(wrapper).toMatchSnapshot();
});

describe('propsToIssueParams', () => {
  it('should render correct default parameters', () => {
    const wrapper = shallowRender();
    expect(wrapper.instance().propsToIssueParams()).toEqual({ resolved: 'false' });
  });

  it(`should render correct params`, () => {
    const wrapper = shallowRender({ metric: 'false_positive_issues', sinceLeakPeriod: true });
    expect(wrapper.instance().propsToIssueParams()).toEqual({
      resolutions: 'FALSE-POSITIVE',
      sinceLeakPeriod: true
    });
  });
});

const shallowRender = (props: Partial<DrilldownLink['props']> = {}, label = 'label') => {
  return shallow<DrilldownLink>(
    <DrilldownLink component="project123" metric="other" {...props}>
      {label}
    </DrilldownLink>
  );
};
