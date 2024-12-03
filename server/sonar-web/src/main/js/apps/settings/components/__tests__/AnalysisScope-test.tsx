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

import { uniq } from 'lodash';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import SettingsServiceMock, {
  DEFAULT_DEFINITIONS_MOCK,
} from '../../../../api/mocks/SettingsServiceMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { AdditionalCategoryComponentProps } from '../AdditionalCategories';
import { AnalysisScope } from '../AnalysisScope';

const handler = new SettingsServiceMock();

const ui = {
  introduction: byText('settings.analysis_scope.wildcards.introduction'),
  docLink: byRole('link', {
    name: 'settings.analysis_scope.wildcards.introduction_link open_in_new_tab',
  }),
};

beforeEach(() => {
  handler.reset();
});

it('renders correctly', async () => {
  renderAnalysisScope();

  expect(await ui.introduction.find()).toBeInTheDocument();
  expect(ui.docLink.get()).toBeInTheDocument();
});

function renderAnalysisScope(overrides: Partial<AdditionalCategoryComponentProps> = {}) {
  const props = {
    definitions: DEFAULT_DEFINITIONS_MOCK,
    categories: uniq(DEFAULT_DEFINITIONS_MOCK.map((d) => d.category)),
    selectedCategory: 'general',
    component: mockComponent(),
    ...overrides,
  };
  return renderComponent(<AnalysisScope {...props} />);
}
