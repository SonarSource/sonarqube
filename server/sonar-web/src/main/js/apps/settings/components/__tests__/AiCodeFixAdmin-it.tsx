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
import { uniq } from 'lodash';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import ComponentsServiceMock from '../../../../api/mocks/ComponentsServiceMock';
import FixSuggestionsServiceMock from '../../../../api/mocks/FixSuggestionsServiceMock';
import SettingsServiceMock, {
  DEFAULT_DEFINITIONS_MOCK,
} from '../../../../api/mocks/SettingsServiceMock';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import { mockComponent, mockComponentRaw } from '../../../../helpers/mocks/component';
import { definitions } from '../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../types/features';
import { AdditionalCategoryComponentProps } from '../AdditionalCategories';
import AiCodeFixAdmin from '../AiCodeFixAdmin';

let settingServiceMock: SettingsServiceMock;
let componentsServiceMock: ComponentsServiceMock;
let fixIssueServiceMock: FixSuggestionsServiceMock;

beforeAll(() => {
  settingServiceMock = new SettingsServiceMock();
  settingServiceMock.setDefinitions(definitions);
  componentsServiceMock = new ComponentsServiceMock();
  fixIssueServiceMock = new FixSuggestionsServiceMock();
});

afterEach(() => {
  settingServiceMock.reset();
});

const ui = {
  codeFixTitle: byRole('heading', { name: 'property.aicodefix.admin.title' }),
  enableAiCodeFixCheckbox: byRole('checkbox', {
    name: 'property.aicodefix.admin.checkbox.label property.aicodefix.admin.terms property.aicodefix.admin.acceptTerm.terms open_in_new_tab',
  }),
  saveButton: byRole('button', { name: 'save' }),
  cancelButton: byRole('button', { name: 'cancel' }),
  checkServiceStatusButton: byRole('button', {
    name: 'property.aicodefix.admin.serviceCheck.action',
  }),
  allProjectsEnabledRadio: byRole('radio', {
    name: 'property.aicodefix.admin.enable.all.projects.label',
  }),
  someProjectsEnabledRadio: byRole('radio', {
    name: 'property.aicodefix.admin.enable.some.projects.label',
  }),
  selectedTab: byRole('radio', { name: 'selected' }),
  unselectedTab: byRole('radio', { name: 'unselected' }),
  allTab: byRole('radio', { name: 'all' }),
};

it('should by default propose enabling for all projects when enabling the feature', async () => {
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'DISABLED');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  expect(await ui.codeFixTitle.find()).toBeInTheDocument();
  expect(ui.enableAiCodeFixCheckbox.get()).not.toBeChecked();

  await user.click(ui.enableAiCodeFixCheckbox.get());
  expect(ui.allProjectsEnabledRadio.get()).toBeEnabled();
});

it('should be able to enable the code fix feature for all projects', async () => {
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'DISABLED');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  expect(await ui.codeFixTitle.find()).toBeInTheDocument();
  expect(ui.enableAiCodeFixCheckbox.get()).not.toBeChecked();

  await user.click(ui.enableAiCodeFixCheckbox.get());
  expect(ui.allProjectsEnabledRadio.get()).toBeEnabled();
  expect(ui.saveButton.get()).toBeEnabled();

  await user.click(ui.saveButton.get());
  expect(ui.enableAiCodeFixCheckbox.get()).toBeChecked();
  await waitFor(() => {
    expect(ui.saveButton.get()).toBeDisabled();
  });
});

it('should be able to enable the code fix feature for some projects', async () => {
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'DISABLED');
  const project = mockComponentRaw({ isAiCodeFixEnabled: false });
  componentsServiceMock.registerProject(project);
  const user = userEvent.setup();
  renderCodeFixAdmin();

  expect(ui.enableAiCodeFixCheckbox.get()).not.toBeChecked();

  await user.click(ui.enableAiCodeFixCheckbox.get());
  expect(ui.someProjectsEnabledRadio.get()).toBeEnabled();
  await user.click(ui.someProjectsEnabledRadio.get());

  expect(ui.selectedTab.get()).toBeVisible();
  expect(await ui.unselectedTab.find()).toBeVisible();
  expect(await ui.allTab.find()).toBeVisible();
  await user.click(ui.unselectedTab.get());
  const projectCheckBox = byText(project.name);
  await waitFor(() => {
    expect(projectCheckBox.get()).toBeVisible();
  });
  await user.click(projectCheckBox.get());

  await user.click(ui.saveButton.get());
  expect(ui.enableAiCodeFixCheckbox.get()).toBeChecked();
  await waitFor(() => {
    expect(ui.saveButton.get()).toBeDisabled();
  });
});

it('should be able to disable the feature for a single project', async () => {
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'ENABLED_FOR_SOME_PROJECTS');
  const project = mockComponentRaw({ isAiCodeFixEnabled: true });
  componentsServiceMock.registerProject(project);
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await waitFor(() => {
    expect(ui.enableAiCodeFixCheckbox.get()).toBeChecked();
  });
  expect(ui.someProjectsEnabledRadio.get()).toBeEnabled();

  // this project is by default registered by the mock
  const projectName = 'sonar-plugin-api';
  const projectCheckBox = byText(projectName);
  expect(await projectCheckBox.find()).toBeInTheDocument();
  await user.click(projectCheckBox.get());

  await user.click(ui.saveButton.get());
  expect(ui.enableAiCodeFixCheckbox.get()).toBeChecked();
  await waitFor(() => {
    expect(ui.saveButton.get()).toBeDisabled();
  });
});

it('should be able to disable the code fix feature', async () => {
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'ENABLED_FOR_ALL_PROJECTS');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await waitFor(() => {
    expect(ui.enableAiCodeFixCheckbox.get()).toBeChecked();
  });

  await user.click(ui.enableAiCodeFixCheckbox.get());
  expect(await ui.saveButton.find()).toBeInTheDocument();
  await user.click(await ui.saveButton.find());
  expect(ui.enableAiCodeFixCheckbox.get()).not.toBeChecked();
});

it('should be able to reset the form when canceling', async () => {
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'ENABLED_FOR_ALL_PROJECTS');
  componentsServiceMock.registerComponent(mockComponent());
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await waitFor(() => {
    expect(ui.enableAiCodeFixCheckbox.get()).toBeChecked();
  });

  await user.click(ui.enableAiCodeFixCheckbox.get());
  expect(ui.enableAiCodeFixCheckbox.get()).not.toBeChecked();
  expect(await ui.cancelButton.find()).toBeInTheDocument();
  await user.click(await ui.cancelButton.find());
  expect(ui.enableAiCodeFixCheckbox.get()).toBeChecked();
});

it('should display a success message when the service status can be successfully checked', async () => {
  fixIssueServiceMock.setServiceStatus('SUCCESS');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await user.click(ui.checkServiceStatusButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.serviceCheck.result.success'),
  ).toBeInTheDocument();
});

it('should display an error message when the service is not responsive', async () => {
  fixIssueServiceMock.setServiceStatus('TIMEOUT');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await user.click(ui.checkServiceStatusButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.serviceCheck.result.unresponsive.message'),
  ).toBeInTheDocument();
});

it('should display an error message when there is a connection error with the service', async () => {
  fixIssueServiceMock.setServiceStatus('CONNECTION_ERROR');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await user.click(ui.checkServiceStatusButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.serviceCheck.result.unresponsive.message'),
  ).toBeInTheDocument();
});

it('should display an error message when the current instance is unauthorized', async () => {
  fixIssueServiceMock.setServiceStatus('UNAUTHORIZED');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await user.click(ui.checkServiceStatusButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.serviceCheck.result.unauthorized'),
  ).toBeInTheDocument();
});

it('should display an error message when an error happens at service level', async () => {
  fixIssueServiceMock.setServiceStatus('SERVICE_ERROR');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await user.click(ui.checkServiceStatusButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.serviceCheck.result.serviceError'),
  ).toBeInTheDocument();
});

it('should display an error message when the service answers with an unknown status', async () => {
  fixIssueServiceMock.setServiceStatus('WTF');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await user.click(ui.checkServiceStatusButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.serviceCheck.result.unknown WTF'),
  ).toBeInTheDocument();
});

it('should display an error message when the backend answers with an error', async () => {
  fixIssueServiceMock.setServiceStatus(undefined);
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await user.click(ui.checkServiceStatusButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.serviceCheck.result.requestError No status'),
  ).toBeInTheDocument();
});

function renderCodeFixAdmin(
  overrides: Partial<AdditionalCategoryComponentProps> = {},
  features?: Feature[],
) {
  const props = {
    definitions: DEFAULT_DEFINITIONS_MOCK,
    categories: uniq(DEFAULT_DEFINITIONS_MOCK.map((d) => d.category)),
    selectedCategory: 'general',
    ...overrides,
  };
  return renderComponent(
    <AvailableFeaturesContext.Provider value={features ?? [Feature.FixSuggestions]}>
      <AiCodeFixAdmin {...props} />
    </AvailableFeaturesContext.Provider>,
  );
}
