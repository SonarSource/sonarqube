/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import * as React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  mockAnalysis,
  mockAnalysisEvent,
  mockMeasureHistory,
} from '../../../../helpers/mocks/project-activity';
import { mockMetric } from '../../../../helpers/testMocks';

import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import {
  ApplicationAnalysisEventCategory,
  DefinitionChangeType,
  ProjectAnalysisEventCategory,
} from '../../../../types/project-activity';
import ActivityPanel, { ActivityPanelProps } from '../ActivityPanel';

it('should render correctly', async () => {
  renderActivityPanel();
  expect(await screen.findByText('event.quality_gate.ERROR')).toBeInTheDocument();
  expect(screen.getByRole('status', { name: 'v1.0' })).toBeInTheDocument();
  expect(screen.getByText(/event.category.OTHER/)).toBeInTheDocument();
  expect(screen.getByText(/event.category.DEFINITION_CHANGE/)).toBeInTheDocument();
  expect(screen.getByText('event.sqUpgrade10.2')).toBeInTheDocument();
});

function renderActivityPanel(props: Partial<ActivityPanelProps> = {}) {
  const mockedMeasureHistory = [mockMeasureHistory()];
  const mockedMetrics = [mockMetric()];
  const mockedAnalysis = [
    mockAnalysis({
      events: [
        mockAnalysisEvent({ key: '1' }),
        mockAnalysisEvent({
          key: '2',
          category: ProjectAnalysisEventCategory.Version,
          name: 'v1.0',
        }),
        mockAnalysisEvent({
          key: '3',
          category: ProjectAnalysisEventCategory.Other,
          name: 'Other',
        }),
        mockAnalysisEvent({
          key: '4',
          category: ApplicationAnalysisEventCategory.DefinitionChange,
          name: 'DefinitionChange',
          definitionChange: {
            projects: [
              {
                changeType: DefinitionChangeType.Added,
                key: 'string',
                name: 'string',
              },
            ],
          },
        }),
        mockAnalysisEvent({
          key: '5',
          category: ProjectAnalysisEventCategory.SqUpgrade,
          name: '10.2',
        }),
      ],
    }),
    mockAnalysis({ key: 'bar' }),
  ];

  const mockedProps: ActivityPanelProps = {
    analyses: mockedAnalysis,
    measuresHistory: mockedMeasureHistory,
    metrics: mockedMetrics,
    component: mockComponent(),
    onGraphChange: jest.fn(),
    loading: false,
  };

  return renderComponent(<ActivityPanel {...mockedProps} {...props} />);
}
