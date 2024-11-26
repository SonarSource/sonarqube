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
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier, Visibility } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { AiCodeAssuredServiceMock } from '../../../api/mocks/AiCodeAssuredServiceMock';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import CodingRulesServiceMock from '../../../api/mocks/CodingRulesServiceMock';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import { MeasuresServiceMock } from '../../../api/mocks/MeasuresServiceMock';
import { ModeServiceMock } from '../../../api/mocks/ModeServiceMock';
import NotificationsMock from '../../../api/mocks/NotificationsMock';
import { ProjectBadgesServiceMock } from '../../../api/mocks/ProjectBadgesServiceMock';
import ProjectLinksServiceMock from '../../../api/mocks/ProjectLinksServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockCurrentUser, mockLoggedInUser, mockMeasure } from '../../../helpers/testMocks';
import {
  renderAppWithComponentContext,
  RenderContext,
} from '../../../helpers/testReactTestingUtils';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import routes from '../routes';

jest.mock('../../../api/rules');
jest.mock('../../../api/issues');
jest.mock('../../../api/quality-profiles');
jest.mock('../../../api/users');
jest.mock('../../../api/web-api', () => ({
  fetchWebApi: () => Promise.resolve([]),
}));

const componentsMock = new ComponentsServiceMock();
const measuresHandler = new MeasuresServiceMock();
const linksHandler = new ProjectLinksServiceMock();
const rulesHandler = new CodingRulesServiceMock();
const badgesHandler = new ProjectBadgesServiceMock();
const notificationsHandler = new NotificationsMock();
const branchesHandler = new BranchesServiceMock();
const aiCodeAssurance = new AiCodeAssuredServiceMock();
const modeHandler = new ModeServiceMock();

const ui = {
  projectPageTitle: byRole('heading', { name: 'project.info.title' }),
  applicationPageTitle: byRole('heading', { name: 'application.info.title' }),
  qualityGateHeader: byRole('heading', { name: 'project.info.quality_gate' }),
  qualityProfilesHeader: byRole('heading', { name: 'overview.quality_profiles' }),
  externalLinksHeader: byRole('heading', { name: 'overview.external_links' }),
  tags: byRole('generic', { name: /tags:/ }),
  size: byRole('link', { name: /project.info.see_more_info_on_x_locs/ }),
  newKeyInput: byRole('textbox'),
  updateInputButton: byRole('button', { name: 'update_verb' }),
  resetInputButton: byRole('button', { name: 'reset_verb' }),
};

afterEach(() => {
  componentsMock.reset();
  measuresHandler.reset();
  linksHandler.reset();
  rulesHandler.reset();
  badgesHandler.reset();
  notificationsHandler.reset();
  branchesHandler.reset();
  aiCodeAssurance.reset();
  modeHandler.reset();
});

it('should show fields for project', async () => {
  measuresHandler.registerComponentMeasures({
    'my-project': { [MetricKey.ncloc]: mockMeasure({ metric: MetricKey.ncloc, value: '1000' }) },
  });
  linksHandler.projectLinks = [{ id: '1', name: 'test', type: '', url: 'http://test.com' }];
  renderProjectInformationApp(
    {
      visibility: Visibility.Private,
      description: 'Test description',
      tags: ['bar'],
    },
    { currentUser: mockLoggedInUser(), featureList: [Feature.AiCodeAssurance] },
  );
  expect(await ui.projectPageTitle.find()).toBeInTheDocument();
  expect(ui.qualityGateHeader.get()).toBeInTheDocument();
  expect(byRole('link', { name: /project.info.quality_gate.link_label/ }).getAll()).toHaveLength(1);
  expect(byRole('link', { name: /overview.link_to_x_profile_y/ }).getAll()).toHaveLength(1);
  expect(byRole('link', { name: 'test' }).getAll()).toHaveLength(1);
  expect(screen.getByText('project.info.ai_code_assurance.title')).toBeInTheDocument();
  expect(screen.getByText('Test description')).toBeInTheDocument();
  expect(screen.getByText('my-project')).toBeInTheDocument();
  expect(screen.getByText('visibility.private')).toBeInTheDocument();
  expect(ui.tags.get()).toHaveTextContent('bar');
  expect(ui.size.get()).toHaveTextContent('1short_number_suffix.k');
});

it('should show application fields', async () => {
  measuresHandler.registerComponentMeasures({
    'my-project': {
      [MetricKey.ncloc]: mockMeasure({ metric: MetricKey.ncloc, value: '1000' }),
      [MetricKey.projects]: mockMeasure({ metric: MetricKey.projects, value: '2' }),
    },
  });
  renderProjectInformationApp(
    {
      qualifier: ComponentQualifier.Application,
      visibility: Visibility.Private,
      description: 'Test description',
      tags: ['bar'],
    },
    { currentUser: mockLoggedInUser() },
  );
  expect(await ui.applicationPageTitle.find()).toBeInTheDocument();
  expect(ui.qualityGateHeader.query()).not.toBeInTheDocument();
  expect(ui.qualityProfilesHeader.query()).not.toBeInTheDocument();
  expect(ui.externalLinksHeader.query()).not.toBeInTheDocument();
  expect(screen.getByText('Test description')).toBeInTheDocument();
  expect(screen.getByText('my-project')).toBeInTheDocument();
  expect(screen.getByText('visibility.private')).toBeInTheDocument();
  expect(ui.tags.get()).toHaveTextContent('bar');
  expect(ui.size.get()).toHaveTextContent('1short_number_suffix.k');
  expect(screen.getByRole('link', { name: '2' })).toBeInTheDocument();
});

it('should hide some fields for application', async () => {
  renderProjectInformationApp(
    {
      qualifier: ComponentQualifier.Application,
    },
    { featureList: [Feature.AiCodeAssurance] },
  );
  expect(await ui.applicationPageTitle.find()).toBeInTheDocument();
  expect(screen.getByText('application.info.empty_description')).toBeInTheDocument();
  expect(screen.queryByText('project.info.ai_code_assurance.title')).not.toBeInTheDocument();
  expect(screen.getByText('visibility.public')).toBeInTheDocument();
  expect(ui.tags.get()).toHaveTextContent('no_tags');
});

it('should not display ai code assurence', async () => {
  renderProjectInformationApp(
    {
      key: 'no-ai',
    },
    { featureList: [Feature.AiCodeAssurance] },
  );
  expect(await ui.projectPageTitle.find()).toBeInTheDocument();
  expect(screen.queryByText('project.info.ai_code_assurance.title')).not.toBeInTheDocument();
});

it('should display ai code fix section if enabled', async () => {
  renderProjectInformationApp({
    isAiCodeFixEnabled: true,
  });
  expect(await ui.projectPageTitle.find()).toBeInTheDocument();
  expect(screen.getByText('project.info.ai_code_fix.title')).toBeInTheDocument();
  expect(screen.getByText('project.info.ai_code_fix.message')).toBeInTheDocument();
});

it('should not display ai code fix section if disabled', async () => {
  renderProjectInformationApp({
    isAiCodeFixEnabled: false,
  });
  expect(await ui.projectPageTitle.find()).toBeInTheDocument();
  expect(screen.queryByText('project.info.ai_code_fix.title')).not.toBeInTheDocument();
  expect(screen.queryByText('project.info.ai_code_fix.message')).not.toBeInTheDocument();
});

it('should not show field that is not configured', async () => {
  renderProjectInformationApp({
    qualityGate: undefined,
    qualityProfiles: [],
  });
  expect(await ui.projectPageTitle.find()).toBeInTheDocument();
  expect(ui.qualityGateHeader.query()).not.toBeInTheDocument();
  expect(ui.qualityProfilesHeader.query()).not.toBeInTheDocument();
  expect(screen.getByText('visibility.public')).toBeInTheDocument();
  expect(ui.tags.get()).toHaveTextContent('no_tags');
  expect(screen.getByText('project.info.empty_description')).toBeInTheDocument();
});

it('should hide visibility if public', async () => {
  renderProjectInformationApp({
    visibility: Visibility.Public,
    qualityGate: undefined,
    qualityProfiles: [],
  });
  expect(await ui.projectPageTitle.find()).toBeInTheDocument();
  expect(ui.qualityGateHeader.query()).not.toBeInTheDocument();
  expect(ui.qualityProfilesHeader.query()).not.toBeInTheDocument();
  expect(screen.getByText('visibility.public')).toBeInTheDocument();
  expect(ui.tags.get()).toHaveTextContent('no_tags');
  expect(screen.getByText('project.info.empty_description')).toBeInTheDocument();
});

function renderProjectInformationApp(
  overrides: Partial<Component> = {},
  context: RenderContext = { currentUser: mockCurrentUser() },
) {
  const component = mockComponent(overrides);
  componentsMock.registerComponent(component, [componentsMock.components[0].component]);
  measuresHandler.setComponents({ component, ancestors: [], children: [] });
  return renderAppWithComponentContext(
    'project/information',
    routes,
    { ...context },
    { component },
  );
}
