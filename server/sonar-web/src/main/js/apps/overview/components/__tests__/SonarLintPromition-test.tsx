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
import { mockQualityGateStatusCondition } from '../../../../helpers/mocks/quality-gates';
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { MetricKey } from '../../../../types/metrics';
import { SonarLintPromotion, SonarLintPromotionProps } from '../SonarLintPromotion';

it('should render correctly', () => {
  expect(shallowRender().type()).toBeNull();
  expect(
    shallowRender({ currentUser: mockCurrentUser({ usingSonarLintConnectedMode: true }) }).type()
  ).toBeNull();
  expect(
    shallowRender({
      qgConditions: [
        mockQualityGateStatusCondition({ metric: MetricKey.new_bugs, level: 'ERROR' }),
      ],
    })
  ).toMatchSnapshot('has failed condition');
});

it.each(
  [
    MetricKey.new_blocker_violations,
    MetricKey.new_critical_violations,
    MetricKey.new_info_violations,
    MetricKey.new_violations,
    MetricKey.new_major_violations,
    MetricKey.new_minor_violations,
    MetricKey.new_code_smells,
    MetricKey.new_bugs,
    MetricKey.new_vulnerabilities,
    MetricKey.new_security_rating,
    MetricKey.new_maintainability_rating,
    MetricKey.new_reliability_rating,
  ].map(Array.of)
)('should show message for %s', (metric) => {
  const wrapper = shallowRender({
    qgConditions: [mockQualityGateStatusCondition({ metric: metric as string })],
  });
  expect(wrapper.type()).not.toBeNull();
});

function shallowRender(props: Partial<SonarLintPromotionProps> = {}) {
  return shallow(<SonarLintPromotion currentUser={mockCurrentUser()} {...props} />);
}
