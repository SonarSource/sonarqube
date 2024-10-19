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
import React from 'react';
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { MeasuresServiceMock } from '../../../../../api/mocks/MeasuresServiceMock';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser, mockMeasure } from '../../../../../helpers/testMocks';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { SettingsKey } from '../../../../../types/settings';
import { CurrentUser } from '../../../../../types/users';
import { Project } from '../../../types';
import ProjectCard from '../ProjectCard';

const MEASURES = {
  [MetricKey.ncloc]: '1000',
  [MetricKey.alert_status]: 'OK',
  [MetricKey.reliability_rating]: '1.0',
  [MetricKey.security_rating]: '1.0',
  [MetricKey.sqale_rating]: '1.0',
  [MetricKey.security_review_rating]: '3.0',
  [MetricKey.new_bugs]: '12',
};

const PROJECT: Project = {
  analysisDate: '2017-01-01',
  key: 'foo',
  measures: MEASURES,
  name: 'Foo',
  qualifier: ComponentQualifier.Project,
  tags: [],
  visibility: Visibility.Public,
  isScannable: false,
  isAiCodeAssured: true,
};

const PROJECT_WITH_AI_CODE_DISABLED: Project = {
  analysisDate: '2017-01-01',
  key: 'foo',
  measures: MEASURES,
  name: 'Foo',
  qualifier: ComponentQualifier.Project,
  tags: [],
  visibility: Visibility.Public,
  isScannable: false,
  isAiCodeAssured: false,
};

const USER_LOGGED_OUT = mockCurrentUser();
const USER_LOGGED_IN = mockLoggedInUser();

const settingsHandler = new SettingsServiceMock();
const measuresHandler = new MeasuresServiceMock();

beforeEach(() => {
  settingsHandler.reset();
  measuresHandler.reset();
});

it('should not display the quality gate', () => {
  const project = { ...PROJECT, analysisDate: undefined };
  renderProjectCard(project);
  expect(screen.getByText('projects.not_analyzed.TRK')).toBeInTheDocument();
});

it('should display tags', async () => {
  const project = { ...PROJECT, tags: ['foo', 'bar'] };
  renderProjectCard(project);
  await expect(screen.getByText('foo')).toHaveATooltipWithContent('foo, bar');
});

it('should display private badge', () => {
  const project: Project = { ...PROJECT, visibility: Visibility.Private };
  renderProjectCard(project);
  expect(screen.getByText('visibility.private')).toBeInTheDocument();
});

it('should display ai code assurance badge when isAiCodeAssured is true', () => {
  const project: Project = { ...PROJECT, visibility: Visibility.Private };
  renderProjectCard(project);
  expect(screen.getByText('ai_code')).toBeInTheDocument();
});

it('should display ai code assurance badge when isAiCodeAssured is false', () => {
  const project: Project = { ...PROJECT_WITH_AI_CODE_DISABLED, visibility: Visibility.Private };
  renderProjectCard(project);
  expect(screen.queryByText('ai_code')).not.toBeInTheDocument();
});

it('should display configure analysis button for logged in user and scan rights', () => {
  const user = mockLoggedInUser();
  renderProjectCard({ ...PROJECT, isScannable: true, analysisDate: undefined }, user);
  expect(screen.getByText('projects.configure_analysis')).toBeInTheDocument();
});

it('should not display configure analysis button for logged in user and without scan rights', () => {
  renderProjectCard({ ...PROJECT, analysisDate: undefined }, USER_LOGGED_IN);
  expect(screen.queryByText('projects.configure_analysis')).not.toBeInTheDocument();
});

it('should display applications', () => {
  renderProjectCard({ ...PROJECT, qualifier: ComponentQualifier.Application });
  expect(screen.getAllByText('qualifier.APP')).toHaveLength(2);
});

it('should display 3 projects', () => {
  renderProjectCard({
    ...PROJECT,
    qualifier: ComponentQualifier.Application,
    measures: { ...MEASURES, projects: '3' },
  });
  expect(screen.getByText(/x_projects_.3/)).toBeInTheDocument();
});

describe('upgrade scenario (awaiting scan)', () => {
  const oldRatings = {
    [MetricKey.reliability_rating]: mockMeasure({
      metric: MetricKey.reliability_rating,
      value: '1',
    }),
    [MetricKey.sqale_rating]: mockMeasure({
      metric: MetricKey.sqale_rating,
      value: '1',
    }),
    [MetricKey.security_rating]: mockMeasure({
      metric: MetricKey.security_rating,
      value: '1',
    }),
    [MetricKey.security_review_rating]: mockMeasure({
      metric: MetricKey.security_review_rating,
      value: '3',
    }),
  };

  const newRatings = {
    [MetricKey.software_quality_reliability_rating]: mockMeasure({
      metric: MetricKey.software_quality_reliability_rating,
      value: '2',
    }),
    [MetricKey.software_quality_maintainability_rating]: mockMeasure({
      metric: MetricKey.software_quality_maintainability_rating,
      value: '2',
    }),
    [MetricKey.software_quality_security_rating]: mockMeasure({
      metric: MetricKey.software_quality_security_rating,
      value: '2',
    }),
    [MetricKey.security_review_rating]: mockMeasure({
      metric: MetricKey.security_review_rating,
      value: '3',
    }),
  };

  beforeEach(() => {
    measuresHandler.setComponents({
      component: mockComponent({ key: PROJECT.key }),
      ancestors: [],
      children: [],
    });
    measuresHandler.registerComponentMeasures({
      [PROJECT.key]: oldRatings,
    });
  });

  it('should not display awaiting analysis badge and do not display old measures', async () => {
    measuresHandler.registerComponentMeasures({
      [PROJECT.key]: {
        ...newRatings,
        ...oldRatings,
      },
    });
    renderProjectCard({
      ...PROJECT,
      measures: {
        ...MEASURES,
        [MetricKey.security_issues]: JSON.stringify({ LOW: 0, MEDIUM: 0, HIGH: 1, total: 1 }),
        [MetricKey.reliability_issues]: JSON.stringify({ LOW: 0, MEDIUM: 2, HIGH: 0, total: 2 }),
        [MetricKey.maintainability_issues]: JSON.stringify({
          LOW: 3,
          MEDIUM: 0,
          HIGH: 0,
          total: 3,
        }),
        [MetricKey.software_quality_maintainability_rating]: '2',
        [MetricKey.software_quality_reliability_rating]: '2',
        [MetricKey.software_quality_security_rating]: '2',
        [MetricKey.code_smells]: '4',
        [MetricKey.bugs]: '5',
        [MetricKey.vulnerabilities]: '6',
      },
    });
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText('A')).toHaveLength(3));
    await waitFor(() => expect(screen.getAllByText('C')).toHaveLength(1));
    expect(screen.queryByText('projects.awaiting_scan')).not.toBeInTheDocument();
    expect(screen.queryByText('4')).not.toBeInTheDocument();
    expect(screen.queryByText('5')).not.toBeInTheDocument();
    expect(screen.queryByText('6')).not.toBeInTheDocument();
    expect(screen.queryByText('B')).not.toBeInTheDocument();
  });

  it('should display awaiting analysis badge and show the old measures', async () => {
    const user = userEvent.setup();
    renderProjectCard({
      ...PROJECT,
      measures: {
        ...MEASURES,
        [MetricKey.code_smells]: '4',
        [MetricKey.bugs]: '5',
        [MetricKey.vulnerabilities]: '6',
      },
    });
    expect(await screen.findByText('projects.awaiting_scan')).toBeInTheDocument();
    await user.click(screen.getByText('projects.awaiting_scan'));
    await expect(screen.getByText('projects.awaiting_scan.description.TRK')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.getAllByText('A')).toHaveLength(3);
    expect(screen.getAllByText('C')).toHaveLength(1);
  });

  it('should display awaiting analysis badge, show new software qualities, but old ratings', async () => {
    renderProjectCard({
      ...PROJECT,
      measures: {
        ...MEASURES,
        [MetricKey.security_issues]: JSON.stringify({ LOW: 0, MEDIUM: 0, HIGH: 1, total: 1 }),
        [MetricKey.reliability_issues]: JSON.stringify({ LOW: 0, MEDIUM: 2, HIGH: 0, total: 2 }),
        [MetricKey.maintainability_issues]: JSON.stringify({
          LOW: 3,
          MEDIUM: 0,
          HIGH: 0,
          total: 3,
        }),
        [MetricKey.code_smells]: '4',
        [MetricKey.bugs]: '5',
        [MetricKey.vulnerabilities]: '6',
      },
    });
    expect(await screen.findByText('1')).toBeInTheDocument();
    expect(screen.queryByText('projects.awaiting_scan')).not.toBeInTheDocument();

    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.queryByText('4')).not.toBeInTheDocument();
    expect(screen.queryByText('5')).not.toBeInTheDocument();
    expect(screen.queryByText('6')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText('A')).toHaveLength(3));
    expect(screen.getAllByText('C')).toHaveLength(1);
  });

  it('should display awaiting analysis badge and show the old measures for Application', async () => {
    const user = userEvent.setup();
    renderProjectCard({
      ...PROJECT,
      qualifier: ComponentQualifier.Application,
      measures: {
        ...MEASURES,
        [MetricKey.code_smells]: '4',
        [MetricKey.bugs]: '5',
        [MetricKey.vulnerabilities]: '6',
      },
    });
    expect(await screen.findByText('projects.awaiting_scan')).toBeInTheDocument();
    await user.click(screen.getByText('projects.awaiting_scan'));
    await expect(screen.getByText('projects.awaiting_scan.description.APP')).toBeInTheDocument();
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
  });

  it('should not display awaiting analysis badge if project is not analyzed', () => {
    renderProjectCard({
      ...PROJECT,
      analysisDate: undefined,
    });
    expect(screen.queryByText('projects.awaiting_scan')).not.toBeInTheDocument();
  });

  it('should not display awaiting analysis badge if project does not have lines of code', () => {
    renderProjectCard({
      ...PROJECT,
      measures: {
        ...(({ [MetricKey.ncloc]: _, ...rest }) => rest)(MEASURES),
      },
    });
    expect(screen.queryByText('projects.awaiting_scan')).not.toBeInTheDocument();
  });

  it('should not display awaiting analysis badge if it is a new code filter', () => {
    renderProjectCard(PROJECT, undefined, 'leak');
    expect(screen.queryByText('projects.awaiting_scan')).not.toBeInTheDocument();
  });

  // eslint-disable-next-line jest/no-disabled-tests
  it.skip('should not display awaiting analysis badge if legacy mode is enabled', async () => {
    settingsHandler.set(SettingsKey.LegacyMode, 'true');
    renderProjectCard({
      ...PROJECT,
      measures: {
        ...MEASURES,
        [MetricKey.code_smells]: '4',
        [MetricKey.bugs]: '5',
        [MetricKey.vulnerabilities]: '6',
      },
    });
    expect(screen.getByText('4')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText('A')).toHaveLength(3));
    expect(screen.getAllByText('C')).toHaveLength(1);
    expect(screen.queryByText('projects.awaiting_scan')).not.toBeInTheDocument();
  });

  // eslint-disable-next-line jest/no-disabled-tests
  it.skip('should not display new values if legacy mode is enabled', async () => {
    settingsHandler.set(SettingsKey.LegacyMode, 'true');
    measuresHandler.registerComponentMeasures({
      [PROJECT.key]: {
        ...newRatings,
        ...oldRatings,
      },
    });
    renderProjectCard({
      ...PROJECT,
      measures: {
        ...MEASURES,
        [MetricKey.security_issues]: JSON.stringify({ LOW: 0, MEDIUM: 0, HIGH: 1, total: 1 }),
        [MetricKey.reliability_issues]: JSON.stringify({ LOW: 0, MEDIUM: 2, HIGH: 0, total: 2 }),
        [MetricKey.maintainability_issues]: JSON.stringify({
          LOW: 3,
          MEDIUM: 0,
          HIGH: 0,
          total: 3,
        }),
        [MetricKey.software_quality_maintainability_rating]: '2',
        [MetricKey.software_quality_reliability_rating]: '2',
        [MetricKey.software_quality_security_rating]: '2',
        [MetricKey.code_smells]: '4',
        [MetricKey.bugs]: '5',
        [MetricKey.vulnerabilities]: '6',
      },
    });
    expect(await screen.findByText('4')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.queryByText('1')).not.toBeInTheDocument();
    expect(screen.queryByText('2')).not.toBeInTheDocument();
    expect(screen.queryByText('3')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText('A')).toHaveLength(3));
    expect(screen.getAllByText('C')).toHaveLength(1);
    expect(screen.queryByText('B')).not.toBeInTheDocument();
    expect(screen.queryByText('projects.awaiting_scan')).not.toBeInTheDocument();
  });
});

function renderProjectCard(project: Project, user: CurrentUser = USER_LOGGED_OUT, type?: string) {
  renderComponent(
    <ProjectCard currentUser={user} handleFavorite={jest.fn()} project={project} type={type} />,
  );
}
