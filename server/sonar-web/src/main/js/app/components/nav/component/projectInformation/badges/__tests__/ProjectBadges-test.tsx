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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import selectEvent from 'react-select-event';
import { getProjectBadgesToken } from '../../../../../../../api/project-badges';
import { mockBranch } from '../../../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../../../helpers/testReactTestingUtils';
import { Location } from '../../../../../../../helpers/urls';
import { ComponentQualifier } from '../../../../../../../types/component';
import { MetricKey } from '../../../../../../../types/metrics';
import ProjectBadges from '../ProjectBadges';
import { BadgeType } from '../utils';

jest.mock('../../../../../../../helpers/urls', () => ({
  getHostUrl: () => 'host',
  getPathUrlAsString: (l: Location) => l.pathname,
  getProjectUrl: () => ({ pathname: '/dashboard' } as Location),
}));

jest.mock('../../../../../../../api/project-badges', () => ({
  getProjectBadgesToken: jest.fn().mockResolvedValue('foo'),
  renewProjectBadgesToken: jest.fn().mockResolvedValue({}),
}));

jest.mock('../../../../../../../api/web-api', () => ({
  fetchWebApi: () =>
    Promise.resolve([
      {
        path: 'api/project_badges',
        actions: [
          {
            key: 'measure',
            // eslint-disable-next-line local-rules/use-metrickey-enum
            params: [{ key: 'metric', possibleValues: ['alert_status', 'coverage'] }],
          },
        ],
      },
    ]),
}));

it('should renew token', async () => {
  const user = userEvent.setup();
  jest.mocked(getProjectBadgesToken).mockResolvedValueOnce('foo').mockResolvedValueOnce('bar');
  renderProjectBadges({
    component: mockComponent({ configuration: { showSettings: true } }),
  });

  expect(
    await screen.findByText(`overview.badges.get_badge.${ComponentQualifier.Project}`)
  ).toHaveFocus();

  expect(screen.getByAltText(`overview.badges.${BadgeType.qualityGate}.alt`)).toHaveAttribute(
    'src',
    'host/api/project_badges/quality_gate?branch=branch-6.7&project=my-project&token=foo'
  );

  expect(screen.getByAltText(`overview.badges.${BadgeType.measure}.alt`)).toHaveAttribute(
    'src',
    'host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=alert_status&token=foo'
  );

  await user.click(screen.getByText('overview.badges.renew'));

  expect(
    await screen.findByAltText(`overview.badges.${BadgeType.qualityGate}.alt`)
  ).toHaveAttribute(
    'src',
    'host/api/project_badges/quality_gate?branch=branch-6.7&project=my-project&token=bar'
  );

  expect(screen.getByAltText(`overview.badges.${BadgeType.measure}.alt`)).toHaveAttribute(
    'src',
    'host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=alert_status&token=bar'
  );
});

it('should update params', async () => {
  renderProjectBadges({
    component: mockComponent({ configuration: { showSettings: true } }),
  });

  expect(
    await screen.findByText(
      '[![alert_status](host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=alert_status&token=foo)](/dashboard)'
    )
  ).toBeInTheDocument();

  await selectEvent.select(screen.getByLabelText('format:'), [
    'overview.badges.options.formats.url',
  ]);

  expect(
    screen.getByText(
      'host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=alert_status&token=foo'
    )
  ).toBeInTheDocument();

  await selectEvent.select(screen.getByLabelText('overview.badges.metric:'), MetricKey.coverage);

  expect(
    screen.getByText(
      `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.coverage}&token=foo`
    )
  ).toBeInTheDocument();
});

function renderProjectBadges(props: Partial<ProjectBadges['props']> = {}) {
  return renderComponent(
    <ProjectBadges
      branchLike={mockBranch()}
      component={mockComponent({ key: 'foo', qualifier: ComponentQualifier.Project })}
      {...props}
    />
  );
}
