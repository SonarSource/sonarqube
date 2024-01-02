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
import { mockBranch, mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { NoCodeWarning } from '../NoCodeWarning';

it('should render correctly if the project has no lines of code', () => {
  const wrapper = shallowRender();
  expect(wrapper.children().text()).toBe('overview.project.main_branch_no_lines_of_code');

  wrapper.setProps({ branchLike: mockBranch({ name: 'branch-foo' }) });
  expect(wrapper.children().text()).toBe('overview.project.branch_X_no_lines_of_code.branch-foo');

  wrapper.setProps({ branchLike: undefined });
  expect(wrapper.children().text()).toBe('overview.project.no_lines_of_code');
});

it('should correctly if the project is empty', () => {
  const wrapper = shallowRender({ measures: [] });
  expect(wrapper.children().text()).toBe('overview.project.main_branch_empty');

  wrapper.setProps({ branchLike: mockBranch({ name: 'branch-foo' }) });
  expect(wrapper.children().text()).toBe('overview.project.branch_X_empty.branch-foo');

  wrapper.setProps({ branchLike: undefined });
  expect(wrapper.children().text()).toBe('overview.project.empty');
});

it('should render correctly if the application is empty or has no lines of code', () => {
  const wrapper = shallowRender({
    component: mockComponent({ qualifier: ComponentQualifier.Application }),
    measures: [mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.projects }) })],
  });
  expect(wrapper.children().text()).toBe('portfolio.app.no_lines_of_code');

  wrapper.setProps({ measures: [] });
  expect(wrapper.children().text()).toBe('portfolio.app.empty');
});

function shallowRender(props = {}) {
  return shallow(
    <NoCodeWarning
      branchLike={mockMainBranch()}
      component={mockComponent()}
      measures={[mockMeasureEnhanced({ metric: mockMetric({ key: MetricKey.bugs }) })]}
      {...props}
    />
  );
}
