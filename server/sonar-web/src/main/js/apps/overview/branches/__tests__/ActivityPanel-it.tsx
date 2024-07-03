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
import * as React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  mockAnalysis,
  mockAnalysisEvent,
  mockHistoryItem,
  mockMeasureHistory,
} from '../../../../helpers/mocks/project-activity';
import { mockMetric } from '../../../../helpers/testMocks';

import userEvent from '@testing-library/user-event';
import { Route, useSearchParams } from 'react-router-dom';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { parseDate } from '../../../../helpers/dates';
import { renderAppRoutes } from '../../../../helpers/testReactTestingUtils';
import {
  ApplicationAnalysisEventCategory,
  DefinitionChangeType,
  ProjectAnalysisEventCategory,
} from '../../../../types/project-activity';
import ActivityPanel, { ActivityPanelProps } from '../ActivityPanel';

it('should render correctly', async () => {
  const user = userEvent.setup();
  renderActivityPanel();

  expect(await screen.findAllByText('metric.level.ERROR')).toHaveLength(2);
  expect(screen.getAllByText('metric.level.OK')).toHaveLength(2);
  expect(screen.getByText('v1.0')).toBeInTheDocument();
  expect(screen.getByText(/event.category.OTHER/)).toBeInTheDocument();
  expect(screen.getByText(/event.category.DEFINITION_CHANGE/)).toBeInTheDocument();
  expect(screen.getByText('event.sqUpgrade10.2')).toBeInTheDocument();

  // Checking measures variations
  expect(screen.getAllByText(/project_activity\.graphs\.coverage$/)).toHaveLength(3);
  expect(screen.getAllByText(/project_activity\.graphs\.duplications$/)).toHaveLength(3);
  // Analysis 1 (latest)
  expect(screen.getByText(/^-5 project_activity\.graphs\.issues$/)).toBeInTheDocument();
  expect(screen.getByText(/^\+6\.5% project_activity\.graphs\.duplications$/)).toBeInTheDocument();
  // Analysis 2
  expect(screen.getByText(/^-100 project_activity\.graphs\.issues$/)).toBeInTheDocument();
  expect(screen.getByText(/^-1\.0% project_activity\.graphs\.coverage$/)).toBeInTheDocument();
  // Analysis 3
  expect(screen.getByText(/^\+0 project_activity\.graphs\.issues$/)).toBeInTheDocument();
  expect(screen.getByText(/^\+15\.2% project_activity\.graphs\.coverage$/)).toBeInTheDocument();
  expect(screen.getByText(/^-1\.5% project_activity\.graphs\.duplications$/)).toBeInTheDocument();
  // Analysis 4 (first one)
  expect(screen.getByText(/^200 project_activity\.graphs\.issues$/)).toBeInTheDocument();
  expect(screen.getByText(/^0\.0% project_activity\.graphs\.coverage$/)).toBeInTheDocument();
  expect(screen.getByText(/^10\.0% project_activity\.graphs\.duplications$/)).toBeInTheDocument();

  // Rich Quality Profile event
  expect(
    screen.getByLabelText(
      /Quality profile QP-test has been updated with 1 new rule, 2 modified rules, and 3 removed rules/,
    ),
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('link', { name: 'quality_profiles.page_title_changelog_x.QP-test' }),
  );

  expect(await screen.findByText('QP-test java')).toBeInTheDocument();
});

function renderActivityPanel() {
  const mockedMeasureHistory = [
    mockMeasureHistory({
      metric: MetricKey.violations,
      history: [
        mockHistoryItem({ date: parseDate('2018-10-27T10:21:15+0200'), value: '200' }),
        mockHistoryItem({ date: parseDate('2018-10-27T12:21:15+0200'), value: '200' }),
        mockHistoryItem({ date: parseDate('2020-10-27T16:33:50+0200'), value: '100' }),
        mockHistoryItem({ date: parseDate('2020-10-27T18:33:50+0200'), value: '95' }),
      ],
    }),
    mockMeasureHistory({
      metric: MetricKey.duplicated_lines_density,
      history: [
        mockHistoryItem({ date: parseDate('2018-10-27T10:21:15+0200'), value: '10.0' }),
        mockHistoryItem({ date: parseDate('2018-10-27T12:21:15+0200'), value: '8.5' }),
        mockHistoryItem({ date: parseDate('2020-10-27T16:33:50+0200'), value: '8.5' }),
        mockHistoryItem({ date: parseDate('2020-10-27T18:33:50+0200'), value: '15.0' }),
      ],
    }),
    mockMeasureHistory({
      metric: MetricKey.coverage,
      history: [
        mockHistoryItem({ date: parseDate('2018-10-27T10:21:15+0200'), value: '0.0' }),
        mockHistoryItem({ date: parseDate('2018-10-27T12:21:15+0200'), value: '15.2' }),
        mockHistoryItem({ date: parseDate('2020-10-27T16:33:50+0200'), value: '14.2' }),
        mockHistoryItem({ date: parseDate('2020-10-27T18:33:50+0200'), value: '14.2' }),
      ],
    }),
    mockMeasureHistory({
      metric: MetricKey.alert_status,
      history: [
        mockHistoryItem({ date: parseDate('2018-10-27T10:21:15+0200'), value: 'OK' }),
        mockHistoryItem({ date: parseDate('2018-10-27T12:21:15+0200'), value: 'ERROR' }),
        mockHistoryItem({ date: parseDate('2020-10-27T16:33:50+0200'), value: 'ERROR' }),
        mockHistoryItem({ date: parseDate('2020-10-27T18:33:50+0200'), value: 'OK' }),
      ],
    }),
  ];
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
        mockAnalysisEvent({
          key: '6',
          category: ProjectAnalysisEventCategory.QualityProfile,
          name: 'Quality profile QP-test has been updated with 1 new rule, 2 modified rules, and 3 removed rules',
          description: '1 new rule, 2 modified rules, and 3 removed rules',
          qualityProfile: {
            languageKey: 'java',
            name: 'QP-test',
            key: 'testkey',
          },
        }),
      ],
    }),
    mockAnalysis({ key: 'bar' }),
    mockAnalysis({ key: 'baz' }),
    mockAnalysis({ key: 'qux' }),
  ];

  const mockedProps: ActivityPanelProps = {
    analyses: mockedAnalysis,
    measuresHistory: mockedMeasureHistory,
    metrics: mockedMetrics,
    component: mockComponent(),
    onGraphChange: jest.fn(),
    loading: false,
  };

  return renderAppRoutes('overview', () => (
    <>
      <Route path="/overview" element={<ActivityPanel {...mockedProps} />} />
      <Route
        path="/profiles/changelog"
        Component={() => {
          const [searchParams] = useSearchParams();
          return (
            <div>
              {searchParams.get('name')} {searchParams.get('language')}
            </div>
          );
        }}
      />
    </>
  ));
}
