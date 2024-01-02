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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { keyBy } from 'lodash';
import React from 'react';
import { Route } from 'react-router-dom';
import { byLabelText, byRole, byText } from 'testing-library-selector';
import { ProjectActivityServiceMock } from '../../../../api/mocks/ProjectActivityServiceMock';
import { getAllTimeMachineData } from '../../../../api/time-machine';
import { mockComponent } from '../../../../helpers/mocks/component';
import { get } from '../../../../helpers/storage';
import { mockMetric, mockPaging } from '../../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { GraphType } from '../../../../types/project-activity';
import ProjectActivityAppContainer from '../ProjectActivityApp';

jest.mock('../../../../api/projectActivity');

jest.mock('../../../../api/time-machine', () => ({
  getAllTimeMachineData: jest.fn(),
}));

jest.mock('../../../../helpers/storage', () => ({
  ...jest.requireActual('../../../../helpers/storage'),
  get: jest.fn(),
}));

let handler: ProjectActivityServiceMock;

beforeAll(() => {
  handler = new ProjectActivityServiceMock();
  (getAllTimeMachineData as jest.Mock).mockResolvedValue({
    measures: [
      {
        metric: MetricKey.reliability_rating,
        history: handler.analysisList.map(({ date }) => ({ date, value: '2.0' })),
      },
      {
        metric: MetricKey.bugs,
        history: handler.analysisList.map(({ date }) => ({ date, value: '10' })),
      },
    ],
    paging: mockPaging(),
  });
});

beforeEach(jest.clearAllMocks);

afterEach(() => handler.reset());

const ui = {
  // Graph types.
  graphTypeIssues: byText('project_activity.graphs.issues'),
  graphTypeCustom: byText('project_activity.graphs.custom'),

  // Add metrics.
  addMetricBtn: byRole('button', { name: 'project_activity.graphs.custom.add' }),
  reviewedHotspotsCheckbox: byRole('checkbox', { name: MetricKey.security_hotspots_reviewed }),
  reviewRatingCheckbox: byRole('checkbox', { name: MetricKey.security_review_rating }),

  // Analysis interactions.
  cogBtn: (id: string) => byRole('button', { name: `project_activity.analysis_X_actions.${id}` }),
  seeDetailsBtn: (time: string) =>
    byRole('button', { name: `project_activity.show_analysis_X_on_graph.${time}` }),
  addCustomEventBtn: byRole('button', { name: 'project_activity.add_custom_event' }),
  addVersionEvenBtn: byRole('button', { name: 'project_activity.add_version' }),
  deleteAnalysisBtn: byRole('button', { name: 'project_activity.delete_analysis' }),
  editEventBtn: byRole('button', { name: 'project_activity.events.tooltip.edit' }),
  deleteEventBtn: byRole('button', { name: 'project_activity.events.tooltip.delete' }),

  // Event modal.
  nameInput: byLabelText('name'),
  saveBtn: byRole('button', { name: 'save' }),
  changeBtn: byRole('button', { name: 'change_verb' }),
  deleteBtn: byRole('button', { name: 'delete' }),

  // Misc.
  loading: byLabelText('loading'),
  baseline: byText('project_activity.new_code_period_start'),
  bugsPopupCell: byRole('cell', { name: 'bugs' }),
};

it('should render issues as default graph', async () => {
  renderProjectActivityAppContainer();

  expect(await ui.graphTypeIssues.find()).toBeInTheDocument();
});

it('should reload custom graph from local storage', async () => {
  (get as jest.Mock).mockImplementation((namespace: string) =>
    // eslint-disable-next-line jest/no-conditional-in-test
    namespace.includes('.custom') ? 'bugs,code_smells' : GraphType.custom
  );
  renderProjectActivityAppContainer();

  expect(await ui.graphTypeCustom.find()).toBeInTheDocument();
});

it.each([
  ['OTHER', ui.addCustomEventBtn, 'Custom event name', 'Custom event updated name'],
  ['VERSION', ui.addVersionEvenBtn, '1.1-SNAPSHOT', '1.1--SNAPSHOT'],
])(
  'should correctly create, update, and delete %s events',
  async (_, btn, initialValue, updatedValue) => {
    const user = userEvent.setup();
    renderProjectActivityAppContainer(
      mockComponent({
        breadcrumbs: [
          { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
        ],
        configuration: { showHistory: true },
      })
    );
    await waitOnDataLoaded();

    await user.click(ui.cogBtn('1.1.0.1').get());
    await user.click(btn.get());
    await user.type(ui.nameInput.get(), initialValue);
    await user.click(ui.saveBtn.get());

    expect(screen.getAllByText(initialValue)[0]).toBeInTheDocument();

    await user.click(ui.editEventBtn.getAll()[1]);
    await user.clear(ui.nameInput.get());
    await user.type(ui.nameInput.get(), updatedValue);
    await user.click(ui.changeBtn.get());

    expect(screen.getAllByText(updatedValue)[0]).toBeInTheDocument();

    await user.click(ui.deleteEventBtn.getAll()[0]);
    await user.click(ui.deleteBtn.get());

    expect(screen.queryByText(updatedValue)).not.toBeInTheDocument();
  }
);

it('should correctly allow deletion of specific analyses', async () => {
  const user = userEvent.setup();
  renderProjectActivityAppContainer(
    mockComponent({
      breadcrumbs: [
        { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
      ],
      configuration: { showHistory: true },
    })
  );
  await waitOnDataLoaded();

  // Most recent analysis is not deletable.
  await user.click(ui.cogBtn('1.1.0.2').get());
  expect(ui.deleteAnalysisBtn.query()).not.toBeInTheDocument();

  await user.click(ui.cogBtn('1.1.0.1').get());
  await user.click(ui.deleteAnalysisBtn.get());
  await user.click(ui.deleteBtn.get());

  expect(screen.queryByText('1.1.0.1')).not.toBeInTheDocument();
});

it('should correctly show the baseline marker', async () => {
  renderProjectActivityAppContainer(
    mockComponent({
      leakPeriodDate: '2017-03-01T10:36:01+0100',
      breadcrumbs: [
        { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project },
      ],
    })
  );
  await waitOnDataLoaded();

  expect(ui.baseline.get()).toBeInTheDocument();
});

it.each([
  [ComponentQualifier.Project, ui.reviewedHotspotsCheckbox, ui.reviewRatingCheckbox],
  [ComponentQualifier.Portfolio, ui.reviewRatingCheckbox, ui.reviewedHotspotsCheckbox],
  [ComponentQualifier.SubPortfolio, ui.reviewRatingCheckbox, ui.reviewedHotspotsCheckbox],
])(
  'should only show certain security hotspot-related metrics for a component with qualifier %s',
  async (qualifier, visible, invisible) => {
    const user = userEvent.setup();
    renderProjectActivityAppContainer(
      mockComponent({
        qualifier,
        breadcrumbs: [{ key: 'breadcrumb', name: 'breadcrumb', qualifier }],
      })
    );

    await user.click(ui.addMetricBtn.get());

    expect(visible.get()).toBeInTheDocument();
    expect(invisible.query()).not.toBeInTheDocument();
  }
);

it('should allow analyses to be clicked', async () => {
  const user = userEvent.setup();
  renderProjectActivityAppContainer();
  await waitOnDataLoaded();

  expect(ui.bugsPopupCell.query()).not.toBeInTheDocument();

  await user.click(ui.seeDetailsBtn('1.0.0.1').get());

  expect(ui.bugsPopupCell.get()).toBeInTheDocument();
});

async function waitOnDataLoaded() {
  await waitFor(() => {
    expect(ui.loading.query()).not.toBeInTheDocument();
  });
}

function renderProjectActivityAppContainer(
  component = mockComponent({
    breadcrumbs: [{ key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project }],
  })
) {
  return renderAppWithComponentContext(
    'project/activity',
    () => <Route path="*" element={<ProjectActivityAppContainer />} />,
    {
      metrics: keyBy(
        [
          mockMetric({ key: MetricKey.bugs, type: 'INT' }),
          mockMetric({ key: MetricKey.security_hotspots_reviewed }),
          mockMetric({ key: MetricKey.security_review_rating, type: 'RATING' }),
        ],
        'key'
      ),
    },
    { component }
  );
}
