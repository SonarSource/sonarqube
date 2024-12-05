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
import { ComponentQualifier } from '~sonar-aligned/types/component';
import AlmSettingsServiceMock from '../../../../../api/mocks/AlmSettingsServiceMock';
import BranchesServiceMock from '../../../../../api/mocks/BranchesServiceMock';
import { MeasuresServiceMock } from '../../../../../api/mocks/MeasuresServiceMock';
import { ModeServiceMock } from '../../../../../api/mocks/ModeServiceMock';
import { mockProjectAlmBindingConfigurationErrors } from '../../../../../helpers/mocks/alm-settings';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { get } from '../../../../../helpers/storage';
import { mockMeasure } from '../../../../../helpers/testMocks';
import { renderApp } from '../../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../../sonar-aligned/helpers/testSelector';
import { MetricKey } from '../../../../../sonar-aligned/types/metrics';
import { Mode } from '../../../../../types/mode';
import ComponentNav, { ComponentNavProps } from '../ComponentNav';

jest.mock('../../../../../helpers/storage', () => ({
  get: jest.fn(),
  remove: jest.fn(),
  save: jest.fn(),
}));

const branchesHandler = new BranchesServiceMock();
const almHandler = new AlmSettingsServiceMock();
const modeHandler = new ModeServiceMock();
const measuresHandler = new MeasuresServiceMock();

afterEach(() => {
  branchesHandler.reset();
  almHandler.reset();
  modeHandler.reset();
  measuresHandler.reset();
});

it('renders correctly when the project binding is incorrect', () => {
  renderComponentNav({
    projectBindingErrors: mockProjectAlmBindingConfigurationErrors(),
  });
  expect(
    screen.getByText('component_navigation.pr_deco.error_detected_X', { exact: false }),
  ).toBeInTheDocument();
});

it('correctly returns focus to the Project Information link when the drawer is closed', async () => {
  const user = userEvent.setup();
  renderComponentNav();
  await user.click(screen.getByRole('link', { name: 'project.info.title' }));
  expect(await screen.findByText('/project/information?id=my-project')).toBeInTheDocument();
});

describe('MQR mode calculation change message', () => {
  it('does not render the message in standard mode', async () => {
    modeHandler.setMode(Mode.Standard);
    renderComponentNav();

    await waitFor(() => {
      expect(screen.queryByText(/overview.missing_project_data/)).not.toBeInTheDocument();
    });
  });

  it.each([
    ['project', ComponentQualifier.Project],
    ['application', ComponentQualifier.Application],
    ['portfolio', ComponentQualifier.Portfolio],
  ])('does not render message when %s is not computed', async (_, qualifier) => {
    const component = mockComponent({
      qualifier,
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier }],
    });
    measuresHandler.registerComponentMeasures({
      [component.key]: {},
    });
    renderComponentNav({ component });

    await waitFor(() => {
      expect(
        byRole('alert')
          .byText(new RegExp(`overview.missing_project_data${qualifier}`))
          .query(),
      ).not.toBeInTheDocument();
    });
  });

  it.each([
    ['project', ComponentQualifier.Project],
    ['application', ComponentQualifier.Application],
    ['portfolio', ComponentQualifier.Portfolio],
  ])('does not render message when %s mqr metrics computed', async (_, qualifier) => {
    const component = mockComponent({
      qualifier,
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier }],
    });
    measuresHandler.registerComponentMeasures({
      [component.key]: {
        [MetricKey.security_rating]: mockMeasure({
          metric: MetricKey.security_rating,
          value: '1.0',
        }),
        [MetricKey.software_quality_security_rating]: mockMeasure({
          metric: MetricKey.software_quality_security_rating,
          value: '1.0',
        }),
      },
    });
    renderComponentNav({ component });

    await waitFor(() => {
      expect(
        byRole('alert')
          .byText(new RegExp(`overview.missing_project_data${qualifier}`))
          .query(),
      ).not.toBeInTheDocument();
    });
  });

  it.each([
    ['project', ComponentQualifier.Project],
    ['application', ComponentQualifier.Application],
    ['portfolio', ComponentQualifier.Portfolio],
  ])(
    'does not render message when %s mqr metrics are not computed but it was already dismissed',
    async (_, qualifier) => {
      const component = mockComponent({
        qualifier,
        breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier }],
      });
      jest.mocked(get).mockImplementation((key) => {
        const keys: Record<string, string> = {
          [`sonarqube.dismissed_calculation_change_alert.component_${component.key}`]: 'true',
        };
        return keys[key];
      });
      measuresHandler.registerComponentMeasures({
        [component.key]: {
          [MetricKey.security_rating]: mockMeasure({
            metric: MetricKey.security_rating,
            value: '1.0',
          }),
        },
      });
      renderComponentNav({ component });

      await waitFor(() => {
        expect(
          byRole('alert')
            .byText(new RegExp(`overview.missing_project_data${qualifier}`))
            .query(),
        ).not.toBeInTheDocument();
      });
      jest.mocked(get).mockRestore();
    },
  );

  it.each([
    ['project', ComponentQualifier.Project],
    ['application', ComponentQualifier.Application],
    ['portfolio', ComponentQualifier.Portfolio],
  ])('renders message when %s mqr metrics are not computed', async (_, qualifier) => {
    const component = mockComponent({
      qualifier,
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier }],
    });
    measuresHandler.registerComponentMeasures({
      [component.key]: {
        [MetricKey.security_rating]: mockMeasure({
          metric: MetricKey.security_rating,
          value: '1.0',
        }),
      },
    });
    renderComponentNav({ component });

    expect(
      await byRole('alert')
        .byText(new RegExp(`overview.missing_project_data${qualifier}`))
        .find(),
    ).toBeInTheDocument();

    expect(
      byRole('link', { name: /overview.missing_project_data_link/ }).get(),
    ).toBeInTheDocument();
  });

  it('can dismiss message', async () => {
    const user = userEvent.setup();

    measuresHandler.registerComponentMeasures({
      'my-project': {
        [MetricKey.security_rating]: mockMeasure({
          metric: MetricKey.security_rating,
          value: '1.0',
        }),
      },
    });
    renderComponentNav();
    expect(
      await byRole('alert')
        .byText(/overview.missing_project_dataTRK/)
        .find(),
    ).toBeInTheDocument();

    await user.click(byRole('button', { name: 'dismiss' }).get());

    expect(
      byRole('alert')
        .byText(/overview.missing_project_dataTRK/)
        .query(),
    ).not.toBeInTheDocument();
  });
});

function renderComponentNav(props: Partial<ComponentNavProps> = {}) {
  const component =
    props.component ??
    mockComponent({
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }],
    });

  measuresHandler.setComponents({ component, ancestors: [], children: [] });

  return renderApp(
    '/',
    <ComponentNav isInProgress={false} isPending={false} {...props} component={component} />,
  );
}
