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
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { uniq } from 'lodash';
import * as React from 'react';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import SettingsServiceMock, {
  DEFAULT_DEFINITIONS_MOCK,
} from '../../../../api/mocks/SettingsServiceMock';
import { AvailableFeaturesContext } from '../../../../app/components/available-features/AvailableFeaturesContext';
import { mockComponent } from '../../../../helpers/mocks/component';
import { definitions } from '../../../../helpers/mocks/definitions-list';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Feature } from '../../../../types/features';
import { AdditionalCategoryComponentProps } from '../AdditionalCategories';
import CodeFixAdmin from '../CodeFixAdmin';

let settingServiceMock: SettingsServiceMock;

beforeAll(() => {
  settingServiceMock = new SettingsServiceMock();
  settingServiceMock.setDefinitions(definitions);
});

afterEach(() => {
  settingServiceMock.reset();
});

const ui = {
  codeFixTitle: byRole('heading', { name: 'property.codefix.admin.title' }),
  changeCodeFixCheckbox: byRole('checkbox', { name: 'property.codefix.admin.checkbox.label' }),
  acceptTermCheckbox: byRole('checkbox', {
    name: 'property.codefix.admin.terms property.codefix.admin.acceptTerm.terms',
  }),
  saveButton: byRole('button', { name: 'save' }),
};

it('should be able to enable the code fix feature', async () => {
  const user = userEvent.setup();
  renderCodeFixAdmin();

  expect(await ui.codeFixTitle.find()).toBeInTheDocument();
  expect(ui.changeCodeFixCheckbox.get()).not.toBeChecked();

  await user.click(ui.changeCodeFixCheckbox.get());
  expect(ui.acceptTermCheckbox.get()).toBeInTheDocument();
  expect(ui.saveButton.get()).toBeDisabled();

  await user.click(ui.acceptTermCheckbox.get());
  expect(ui.saveButton.get()).toBeEnabled();

  await user.click(ui.saveButton.get());
  expect(ui.changeCodeFixCheckbox.get()).toBeChecked();
});

it('should be able to disable the code fix feature', async () => {
  settingServiceMock.set('sonar.ai.suggestions.enabled', 'true');
  const user = userEvent.setup();
  renderCodeFixAdmin();

  await waitFor(() => {
    expect(ui.changeCodeFixCheckbox.get()).toBeChecked();
  });

  await user.click(ui.changeCodeFixCheckbox.get());
  expect(await ui.saveButton.find()).toBeInTheDocument();
  await user.click(await ui.saveButton.find());
  expect(ui.changeCodeFixCheckbox.get()).not.toBeChecked();
});

function renderCodeFixAdmin(
  overrides: Partial<AdditionalCategoryComponentProps> = {},
  features?: Feature[],
) {
  const props = {
    definitions: DEFAULT_DEFINITIONS_MOCK,
    categories: uniq(DEFAULT_DEFINITIONS_MOCK.map((d) => d.category)),
    selectedCategory: 'general',
    component: mockComponent(),
    ...overrides,
  };
  return renderComponent(
    <AvailableFeaturesContext.Provider value={features ?? [Feature.FixSuggestions]}>
      <CodeFixAdmin {...props} />
    </AvailableFeaturesContext.Provider>,
  );
}
