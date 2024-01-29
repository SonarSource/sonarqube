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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockQualityGateStatusConditionEnhanced } from '../../../../helpers/mocks/quality-gates';
import { mockMeasureEnhanced, mockMetric } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { QualityGateStatusConditionEnhanced } from '../../../../types/quality-gates';
import { QualityGateConditions, QualityGateConditionsProps } from '../QualityGateConditions';

const ALL_CONDITIONS = 10;
const HALF_CONDITIONS = 5;

it('should render correctly', async () => {
  renderQualityGateConditions();
  expect(await screen.findAllByText(/.*metric..+.name.*/)).toHaveLength(ALL_CONDITIONS);

  expect(await screen.findAllByText('quality_gates.operator', { exact: false })).toHaveLength(
    ALL_CONDITIONS,
  );
});

it('should be collapsible', async () => {
  renderQualityGateConditions({ collapsible: true });
  const user = userEvent.setup();

  expect(await screen.findAllByText(/.*metric..+.name.*/)).toHaveLength(HALF_CONDITIONS);
  expect(await screen.findAllByText('quality_gates.operator', { exact: false })).toHaveLength(
    HALF_CONDITIONS,
  );

  await user.click(screen.getByRole('link', { name: 'show_more' }));

  expect(await screen.findAllByText(/.*metric..+.name.*/)).toHaveLength(ALL_CONDITIONS);
  expect(await screen.findAllByText('quality_gates.operator', { exact: false })).toHaveLength(
    ALL_CONDITIONS,
  );
});

function renderQualityGateConditions(props: Partial<QualityGateConditionsProps> = {}) {
  const conditions: QualityGateStatusConditionEnhanced[] = [];
  for (let i = ALL_CONDITIONS; i > 0; --i) {
    conditions.push(
      mockQualityGateStatusConditionEnhanced({
        measure: mockMeasureEnhanced({ metric: mockMetric({ key: i.toString() }) }),
      }),
    );
  }

  return renderComponent(
    <QualityGateConditions component={mockComponent()} failedConditions={conditions} {...props} />,
  );
}
