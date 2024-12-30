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
import ComponentsServiceMock from '../../../../../api/mocks/ComponentsServiceMock';
import FixSuggestionsServiceMock from '../../../../../api/mocks/FixSuggestionsServiceMock';
import SettingsServiceMock, {
  DEFAULT_DEFINITIONS_MOCK,
} from '../../../../../api/mocks/SettingsServiceMock';
import { AvailableFeaturesContext } from '../../../../../app/components/available-features/AvailableFeaturesContext';
import { mockComponent, mockComponentRaw } from '../../../../../helpers/mocks/component';
import { definitions } from '../../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../../types/features';
import { AdditionalCategoryComponentProps } from '../../AdditionalCategories';
import AiCodeFixAdmin from '../AiCodeFixAdminCategory';

let settingServiceMock: SettingsServiceMock;
let componentsServiceMock: ComponentsServiceMock;
let fixSuggestionsServiceMock: FixSuggestionsServiceMock;

beforeAll(() => {
  settingServiceMock = new SettingsServiceMock();
  settingServiceMock.setDefinitions(definitions);
  componentsServiceMock = new ComponentsServiceMock();
  fixSuggestionsServiceMock = new FixSuggestionsServiceMock();
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
  retryButton: byRole('button', {
    name: 'property.aicodefix.admin.serviceInfo.result.error.retry.action',
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

it('should display the enablement form when having a paid subscription', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SUCCESS', subscriptionType: 'PAID' });
  renderCodeFixAdmin();

  expect(await screen.findByText('property.aicodefix.admin.description')).toBeInTheDocument();
});

it('should display the enablement form and disclaimer when in early access', async () => {
  fixSuggestionsServiceMock.setServiceInfo({
    isEnabled: true,
    status: 'SUCCESS',
    subscriptionType: 'EARLY_ACCESS',
  });
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.early_access.content1'),
  ).toBeInTheDocument();
});

it('should display a disabled message when in early access and disabled by the customer', async () => {
  fixSuggestionsServiceMock.setServiceInfo({
    isEnabled: false,
    status: 'UNAUTHORIZED',
    subscriptionType: 'EARLY_ACCESS',
  });
  renderCodeFixAdmin();

  expect(await screen.findByText('property.aicodefix.admin.disabled')).toBeInTheDocument();
});

it('should promote the feature when not paid', async () => {
  fixSuggestionsServiceMock.setServiceInfo({
    status: 'UNAUTHORIZED',
    subscriptionType: 'NOT_PAID',
  });
  renderCodeFixAdmin();

  expect(await screen.findByText('property.aicodefix.admin.promotion.content')).toBeInTheDocument();
});

it('should display an error message when the subscription is unknown', async () => {
  fixSuggestionsServiceMock.setServiceInfo({
    status: 'SUCCESS',
    subscriptionType: 'WTF',
  });
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.unexpected.response.label'),
  ).toBeInTheDocument();
});

it('should display an error message when the service is not responsive', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'TIMEOUT' });
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.result.unresponsive.message'),
  ).toBeInTheDocument();
});

it('should display an error message when there is a connection error with the service', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'CONNECTION_ERROR' });
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.result.unresponsive.message'),
  ).toBeInTheDocument();
});

it('should propose to retry when an error occurs', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'CONNECTION_ERROR' });
  const user = userEvent.setup();
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.result.unresponsive.message'),
  ).toBeInTheDocument();
  expect(ui.retryButton.get()).toBeEnabled();

  fixSuggestionsServiceMock.setServiceInfo({
    isEnabled: true,
    status: 'SUCCESS',
    subscriptionType: 'EARLY_ACCESS',
  });
  await user.click(ui.retryButton.get());

  expect(
    await screen.findByText('property.aicodefix.admin.early_access.content1'),
  ).toBeInTheDocument();
});

it('should display an error message when the current instance is unauthorized', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'UNAUTHORIZED' });
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.result.unauthorized'),
  ).toBeInTheDocument();
});

it('should display an error message when an error happens at service level', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SERVICE_ERROR' });
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.result.serviceError'),
  ).toBeInTheDocument();
});

it('should display an error message when the service answers with an unknown status', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'WTF' });
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.result.unknown WTF'),
  ).toBeInTheDocument();
});

it('should display an error message when the backend answers with an error', async () => {
  fixSuggestionsServiceMock.setServiceInfo(undefined);
  renderCodeFixAdmin();

  expect(
    await screen.findByText('property.aicodefix.admin.serviceInfo.result.requestError No status'),
  ).toBeInTheDocument();
});

it('should by default propose enabling for all projects when enabling the feature', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SUCCESS', subscriptionType: 'PAID' });
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'DISABLED');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  expect(await ui.codeFixTitle.find()).toBeInTheDocument();
  expect(ui.enableAiCodeFixCheckbox.get()).not.toBeChecked();

  await user.click(ui.enableAiCodeFixCheckbox.get());
  expect(ui.allProjectsEnabledRadio.get()).toBeEnabled();
});

it('should be able to enable the code fix feature for all projects', async () => {
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SUCCESS', subscriptionType: 'PAID' });
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
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SUCCESS', subscriptionType: 'PAID' });
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'DISABLED');
  const project = mockComponentRaw({ isAiCodeFixEnabled: false });
  componentsServiceMock.registerProject(project);
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await waitFor(() => {
    expect(ui.enableAiCodeFixCheckbox.get()).not.toBeChecked();
  });

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
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SUCCESS', subscriptionType: 'PAID' });
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
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SUCCESS', subscriptionType: 'PAID' });
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
  fixSuggestionsServiceMock.setServiceInfo({ status: 'SUCCESS', subscriptionType: 'PAID' });
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
