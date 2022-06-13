/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import React from 'react';
import { ComponentContext } from '../../../../app/components/componentContext/ComponentContext';
import { getActivityGraph } from '../../../../components/activity-graph/utils';
import { mockComponent } from '../../../../helpers/mocks/component';
import { renderComponentApp } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { Component } from '../../../../types/types';
import ProjectActivityAppContainer from '../ProjectActivityAppContainer';

jest.mock('../../../../api/time-machine', () => {
  const { mockPaging } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getAllTimeMachineData: jest.fn().mockResolvedValue({
      measures: [
        {
          metric: 'bugs',
          history: [{ date: '2022-01-01', value: '10' }]
        }
      ],
      paging: mockPaging({ total: 1 })
    })
  };
});

jest.mock('../../../../api/metrics', () => {
  const { mockMetric } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getAllMetrics: jest.fn().mockResolvedValue([mockMetric()])
  };
});

jest.mock('../../../../api/projectActivity', () => {
  const { mockAnalysis, mockPaging } = jest.requireActual('../../../../helpers/testMocks');
  return {
    ...jest.requireActual('../../../../api/projectActivity'),
    createEvent: jest.fn(),
    changeEvent: jest.fn(),
    getProjectActivity: jest.fn().mockResolvedValue({
      analyses: [mockAnalysis({ key: 'foo' })],
      paging: mockPaging({ total: 1 })
    })
  };
});

jest.mock('../../../../components/activity-graph/utils', () => {
  const actual = jest.requireActual('../../../../components/activity-graph/utils');
  return {
    ...actual,
    getActivityGraph: jest.fn()
  };
});

it('should render default graph', async () => {
  (getActivityGraph as jest.Mock).mockImplementation(() => {
    return {
      graph: 'issues'
    };
  });

  renderProjectActivityAppContainer();

  expect(await screen.findByText('project_activity.graphs.issues')).toBeInTheDocument();
});

it('should reload custom graph from local storage', async () => {
  (getActivityGraph as jest.Mock).mockImplementation(() => {
    return {
      graph: 'custom',
      customGraphs: ['bugs', 'code_smells']
    };
  });

  renderProjectActivityAppContainer();

  expect(await screen.findByText('project_activity.graphs.custom')).toBeInTheDocument();
});

function renderProjectActivityAppContainer(
  { component, navigateTo }: { component: Component; navigateTo?: string } = {
    component: mockComponent({
      breadcrumbs: [
        { key: 'breadcrumb', name: 'breadcrumb', qualifier: ComponentQualifier.Project }
      ]
    })
  }
) {
  return renderComponentApp(
    'project/activity',
    <ComponentContext.Provider
      value={{
        branchLikes: [],
        onBranchesChange: jest.fn(),
        onComponentChange: jest.fn(),
        component
      }}>
      <ProjectActivityAppContainer />
    </ComponentContext.Provider>,
    { navigateTo }
  );
}
