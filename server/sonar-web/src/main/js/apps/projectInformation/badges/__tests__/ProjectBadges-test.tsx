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
import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { getProjectBadgesToken } from '../../../../api/project-badges';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Location } from '../../../../helpers/urls';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import ProjectBadges, { ProjectBadgesProps } from '../ProjectBadges';
import { BadgeType } from '../utils';

jest.mock('../../../../helpers/urls', () => ({
  getHostUrl: () => 'host',
  getPathUrlAsString: (l: Location) => l.pathname,
  getProjectUrl: () => ({ pathname: '/dashboard' }) as Location,
}));

jest.mock('../../../../api/project-badges', () => ({
  getProjectBadgesToken: jest.fn().mockResolvedValue('foo'),
  renewProjectBadgesToken: jest.fn().mockResolvedValue({}),
}));

jest.mock('../../../../api/web-api', () => ({
  fetchWebApi: jest.fn().mockResolvedValue([
    {
      path: 'api/project_badges',
      actions: [
        {
          key: 'measure',
          // eslint-disable-next-line local-rules/use-metrickey-enum
          params: [{ key: 'metric', possibleValues: ['alert_status', 'coverage', 'bugs'] }],
        },
      ],
    },
  ]),
}));

it('should renew token', async () => {
  const user = userEvent.setup();
  jest.mocked(getProjectBadgesToken).mockResolvedValueOnce('foo').mockResolvedValueOnce('bar');
  renderProjectBadges();
  await appLoaded();

  expect(screen.getByAltText(`overview.badges.${BadgeType.measure}.alt`)).toHaveAttribute(
    'src',
    `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.alert_status}&token=foo`,
  );

  await user.click(screen.getByText('overview.badges.renew'));

  expect(
    await screen.findByAltText(`overview.badges.${BadgeType.qualityGate}.alt`),
  ).toHaveAttribute(
    'src',
    'host/api/project_badges/quality_gate?branch=branch-6.7&project=my-project&token=bar',
  );

  expect(screen.getByAltText(`overview.badges.${BadgeType.measure}.alt`)).toHaveAttribute(
    'src',
    `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.alert_status}&token=bar`,
  );
});

it('should update params', async () => {
  renderProjectBadges();
  await appLoaded();

  expect(
    screen.getByText(
      `[![${MetricKey.alert_status}](host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.alert_status}&token=foo)](/dashboard)`,
    ),
  ).toBeInTheDocument();

  await act(async () => {
    await selectEvent.select(
      screen.getByLabelText('overview.badges.format'),
      'overview.badges.options.formats.url',
    );
  });

  expect(
    screen.getByText(
      `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.alert_status}&token=foo`,
    ),
  ).toBeInTheDocument();

  await act(async () => {
    await selectEvent.openMenu(screen.getByLabelText('overview.badges.metric'));
  });
  fireEvent.click(screen.getByText(`metric.${MetricKey.coverage}.name`));

  expect(
    screen.getByText(
      `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.coverage}&token=foo`,
    ),
  ).toBeInTheDocument();

  fireEvent.click(
    screen.getByRole('button', {
      name: `overview.badges.${BadgeType.qualityGate}.alt overview.badges.${BadgeType.qualityGate}.description.${ComponentQualifier.Project}`,
    }),
  );

  expect(
    screen.getByText(
      `host/api/project_badges/quality_gate?branch=branch-6.7&project=my-project&token=foo`,
    ),
  ).toBeInTheDocument();

  fireEvent.click(
    screen.getByRole('button', {
      name: `overview.badges.${BadgeType.measure}.alt overview.badges.${BadgeType.measure}.description.${ComponentQualifier.Project}`,
    }),
  );

  expect(
    screen.getByText(
      `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.coverage}&token=foo`,
    ),
  ).toBeInTheDocument();
});

it('should warn about deprecated metrics', async () => {
  renderProjectBadges();
  await appLoaded();

  await act(async () => {
    await selectEvent.openMenu(screen.getByLabelText('overview.badges.metric'));
  });
  fireEvent.click(screen.getByText(`metric.${MetricKey.bugs}.name (deprecated)`));

  expect(
    screen.getByText(
      `overview.badges.deprecated_badge_x_y.metric.${MetricKey.bugs}.name.qualifier.${ComponentQualifier.Project}`,
    ),
  ).toBeInTheDocument();
});

async function appLoaded() {
  await waitFor(() => expect(screen.queryByLabelText(`loading`)).not.toBeInTheDocument());
}

function renderProjectBadges(props: Partial<ProjectBadgesProps> = {}) {
  return renderComponent(
    <ProjectBadges
      branchLike={mockBranch()}
      component={mockComponent({ configuration: { showSettings: true } })}
      {...props}
    />,
  );
}
