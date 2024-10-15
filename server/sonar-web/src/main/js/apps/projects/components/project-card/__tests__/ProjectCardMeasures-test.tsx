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

import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import SettingsServiceMock from '../../../../../api/mocks/SettingsServiceMock';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { SettingsKey } from '../../../../../types/settings';
import { Dict } from '../../../../../types/types';
import ProjectCardMeasures, { ProjectCardMeasuresProps } from '../ProjectCardMeasures';

jest.mock('date-fns', () => ({
  ...jest.requireActual('date-fns'),
  differenceInMilliseconds: () => 1000 * 60 * 60 * 24 * 30 * 8, // ~ 8 months
}));

const settingsService = new SettingsServiceMock();

beforeEach(() => {
  settingsService.reset();
});

describe('Overall measures', () => {
  it('should be rendered properly', async () => {
    renderProjectCardMeasures();
    expect(await screen.findByTitle('metric.security_issues.short_name')).toBeInTheDocument();
    expect(screen.getByTitle('metric.reliability_issues.short_name')).toBeInTheDocument();
    expect(screen.getByTitle('metric.maintainability_issues.short_name')).toBeInTheDocument();
    expect(screen.queryByTitle('metric.vulnerabilities.short_name')).not.toBeInTheDocument();
    expect(screen.queryByTitle('metric.bugs.short_name')).not.toBeInTheDocument();
    expect(screen.queryByTitle('metric.code_smells.short_name')).not.toBeInTheDocument();
  });

  it('should be rendered properly in Standard mode', async () => {
    settingsService.set(SettingsKey.MQRMode, 'false');
    renderProjectCardMeasures();
    expect(await screen.findByTitle('metric.vulnerabilities.short_name')).toBeInTheDocument();
    expect(screen.getByTitle('metric.bugs.short_name')).toBeInTheDocument();
    expect(screen.getByTitle('metric.code_smells.short_name')).toBeInTheDocument();
    expect(screen.queryByTitle('metric.security_issues.short_name')).not.toBeInTheDocument();
    expect(screen.queryByTitle('metric.reliability_issues.short_name')).not.toBeInTheDocument();
    expect(screen.queryByTitle('metric.maintainability_issues.short_name')).not.toBeInTheDocument();
  });

  it("should be not be rendered if there's no line of code", () => {
    renderProjectCardMeasures({ [MetricKey.ncloc]: undefined });
    expect(screen.getByText('overview.project.main_branch_empty')).toBeInTheDocument();
  });

  it("should be not be rendered if there's no line of code and application", () => {
    renderProjectCardMeasures(
      { [MetricKey.ncloc]: undefined },
      { componentQualifier: ComponentQualifier.Application },
    );
    expect(screen.getByText('portfolio.app.empty')).toBeInTheDocument();
  });
});

describe('New code measures', () => {
  it('should be rendered properly', () => {
    renderProjectCardMeasures({}, { isNewCode: true });
    expect(screen.getByTitle('metric.new_violations.description')).toBeInTheDocument();
  });
});

function renderProjectCardMeasures(
  measuresOverride: Dict<string | undefined> = {},
  props: Partial<ProjectCardMeasuresProps> = {},
) {
  const measures = {
    [MetricKey.alert_status]: 'ERROR',
    [MetricKey.bugs]: '17',
    [MetricKey.code_smells]: '132',
    [MetricKey.coverage]: '88.3',
    [MetricKey.duplicated_lines_density]: '9.8',
    [MetricKey.maintainability_issues]: JSON.stringify({ total: 10 }),
    [MetricKey.reliability_issues]: JSON.stringify({ total: 10 }),
    [MetricKey.security_issues]: JSON.stringify({ total: 10 }),
    [MetricKey.ncloc]: '2053',
    [MetricKey.reliability_rating]: '1.0',
    [MetricKey.security_rating]: '1.0',
    [MetricKey.sqale_rating]: '1.0',
    [MetricKey.vulnerabilities]: '0',
    [MetricKey.new_reliability_rating]: '1.0',
    [MetricKey.new_bugs]: '8',
    [MetricKey.new_security_rating]: '2.0',
    [MetricKey.new_vulnerabilities]: '2',
    [MetricKey.new_maintainability_rating]: '1.0',
    [MetricKey.new_code_smells]: '0',
    [MetricKey.new_coverage]: '26.55',
    [MetricKey.new_duplicated_lines_density]: '0.55',
    [MetricKey.new_violations]: '10',
    [MetricKey.new_lines]: '87',
    ...measuresOverride,
  };

  renderComponent(
    <ProjectCardMeasures
      componentKey="test"
      componentQualifier={ComponentQualifier.Project}
      isNewCode={false}
      measures={measures}
      {...props}
    />,
  );
}
