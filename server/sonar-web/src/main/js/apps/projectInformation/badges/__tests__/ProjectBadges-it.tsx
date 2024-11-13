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
import { MetricKey } from '~sonar-aligned/types/metrics';
import { ProjectBadgesServiceMock } from '../../../../api/mocks/ProjectBadgesServiceMock';
import SettingsServiceMock from '../../../../api/mocks/SettingsServiceMock';
import WebApiServiceMock from '../../../../api/mocks/WebApiServiceMock';
import { getProjectBadgesToken } from '../../../../api/project-badges';
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Location } from '../../../../helpers/urls';
import { byLabelText, byRole, byText } from '../../../../sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '../../../../sonar-aligned/types/component';
import { SettingsKey } from '../../../../types/settings';
import ProjectBadges, { ProjectBadgesProps } from '../ProjectBadges';
import { BadgeType } from '../utils';

jest.mock('../../../../helpers/urls', () => ({
  getHostUrl: () => 'host',
  getPathUrlAsString: (l: Location) => l.pathname,
  getProjectUrl: () => ({ pathname: '/dashboard' }) as Location,
}));

const badgesHandler = new ProjectBadgesServiceMock();
const webApiHandler = new WebApiServiceMock();
const settingsHandler = new SettingsServiceMock();

afterEach(() => {
  badgesHandler.reset();
  webApiHandler.reset();
  settingsHandler.reset();
});

it('should renew token', async () => {
  const { user, ui } = getPageObjects();
  jest.mocked(getProjectBadgesToken).mockResolvedValueOnce('foo').mockResolvedValueOnce('bar');
  renderProjectBadges();
  await ui.appLoaded();

  expect(screen.getByAltText(`overview.badges.${BadgeType.measure}.alt`)).toHaveAttribute(
    'src',
    expect.stringContaining(
      `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.alert_status}&token=foo`,
    ),
  );

  await user.click(screen.getByText('overview.badges.renew'));

  expect(
    await screen.findByAltText(`overview.badges.${BadgeType.qualityGate}.alt`),
  ).toHaveAttribute(
    'src',
    expect.stringContaining(
      'host/api/project_badges/quality_gate?branch=branch-6.7&project=my-project&token=bar',
    ),
  );

  expect(screen.getByAltText(`overview.badges.${BadgeType.measure}.alt`)).toHaveAttribute(
    'src',
    expect.stringContaining(
      `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${MetricKey.alert_status}&token=bar`,
    ),
  );
});

it('can select badges in Standard Experience Mode', async () => {
  const { user, ui } = getPageObjects();
  settingsHandler.set(SettingsKey.MQRMode, 'false');

  renderProjectBadges();
  await ui.appLoaded();

  expect(ui.markdownCode(MetricKey.alert_status).get()).toBeInTheDocument();

  await ui.selectMetric(MetricKey.code_smells);
  expect(ui.markdownCode(MetricKey.code_smells).get()).toBeInTheDocument();

  await ui.selectMetric(MetricKey.security_rating);
  expect(ui.markdownCode(MetricKey.security_rating).get()).toBeInTheDocument();

  await user.click(ui.imageUrlRadio.get());
  expect(ui.urlCode(MetricKey.security_rating).get()).toBeInTheDocument();

  await user.click(ui.qualityGateBadge.get());
  expect(ui.urlCode().get()).toBeInTheDocument();

  await user.click(ui.mardownRadio.get());
  expect(ui.markdownCode().get()).toBeInTheDocument();
});

it('can select badges in MQR Mode', async () => {
  const { user, ui } = getPageObjects();

  renderProjectBadges();
  await ui.appLoaded();

  expect(ui.markdownCode(MetricKey.alert_status).get()).toBeInTheDocument();

  await ui.selectMetric(MetricKey.coverage);
  expect(ui.markdownCode(MetricKey.coverage).get()).toBeInTheDocument();

  await ui.selectMetric(MetricKey.software_quality_reliability_issues);
  expect(ui.markdownCode(MetricKey.software_quality_reliability_issues).get()).toBeInTheDocument();

  await ui.selectMetric(MetricKey.software_quality_maintainability_rating);
  expect(
    ui.markdownCode(MetricKey.software_quality_maintainability_rating).get(),
  ).toBeInTheDocument();

  await user.click(ui.imageUrlRadio.get());
  expect(ui.urlCode(MetricKey.software_quality_maintainability_rating).get()).toBeInTheDocument();

  await user.click(ui.qualityGateBadge.get());
  expect(ui.urlCode().get()).toBeInTheDocument();

  await user.click(ui.mardownRadio.get());
  expect(ui.markdownCode().get()).toBeInTheDocument();
});

const getPageObjects = () => {
  const user = userEvent.setup();

  return {
    user,
    ui: {
      qualityGateBadge: byRole('button', {
        name: `overview.badges.${BadgeType.qualityGate}.alt overview.badges.${BadgeType.qualityGate}.description.${ComponentQualifier.Project}`,
      }),
      imageUrlRadio: byRole('radio', { name: 'overview.badges.options.formats.url' }),
      mardownRadio: byRole('radio', { name: 'overview.badges.options.formats.md' }),
      urlCode: (metric?: MetricKey) =>
        byText(
          metric
            ? `host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${metric}&token=${badgesHandler.token}`
            : `host/api/project_badges/quality_gate?branch=branch-6.7&project=my-project&token=${badgesHandler.token}`,
          { exact: false },
        ),
      markdownCode: (metric?: MetricKey) =>
        byText(
          metric
            ? `[![${metric}](host/api/project_badges/measure?branch=branch-6.7&project=my-project&metric=${metric}&token=${badgesHandler.token}`
            : `[![Quality gate](host/api/project_badges/quality_gate?branch=branch-6.7&project=my-project&token=${badgesHandler.token}`,
          { exact: false },
        ),

      async selectMetric(metric: MetricKey) {
        await user.click(byLabelText('overview.badges.metric').get());
        await user.click(byText(`metric.${metric}.name`).get());
      },
      async appLoaded() {
        await waitFor(() => expect(screen.queryByLabelText(`loading`)).not.toBeInTheDocument());
      },
    },
  };
};

function renderProjectBadges(props: Partial<ProjectBadgesProps> = {}) {
  return renderComponent(
    <ProjectBadges
      branchLike={mockBranch()}
      component={mockComponent({ configuration: { showSettings: true } })}
      {...props}
    />,
  );
}
