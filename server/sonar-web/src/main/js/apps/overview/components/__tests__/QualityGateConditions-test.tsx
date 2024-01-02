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
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQualityGateStatusConditionEnhanced } from '../../../../helpers/mocks/quality-gates';
import { click } from '../../../../helpers/testUtils';
import { QualityGateStatusConditionEnhanced } from '../../../../types/quality-gates';
import { QualityGateConditions, QualityGateConditionsProps } from '../QualityGateConditions';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('QualityGateCondition').length).toBe(10);
});

it('should be collapsible', () => {
  const wrapper = shallowRender({ collapsible: true });
  expect(wrapper.find('QualityGateCondition').length).toBe(5);
  click(wrapper.find('ButtonLink'));
  expect(wrapper.find('QualityGateCondition').length).toBe(10);
});

function shallowRender(props: Partial<QualityGateConditionsProps> = {}) {
  const conditions: QualityGateStatusConditionEnhanced[] = [];
  for (let i = 10; i > 0; --i) {
    conditions.push(mockQualityGateStatusConditionEnhanced());
  }

  return shallow(
    <QualityGateConditions component={mockComponent()} failedConditions={conditions} {...props} />
  );
}
