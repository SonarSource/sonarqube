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
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { byRole } from '../../../helpers/testSelector';
import routes from '../routes';

const componentsMock = new ComponentsServiceMock();

const ui = {
  projectPageTitle: byRole('heading', { name: 'project.info.title' }),
  applicationPageTitle: byRole('heading', { name: 'application.info.title' }),
  qualityGateList: byRole('list', { name: 'project.info.quality_gate' }),
  qualityProfilesList: byRole('list', { name: 'project.info.qualit_profiles' }),
  link: byRole('link'),
  tags: byRole('generic', { name: /tags:/ }),
  size: byRole('link', { name: /project.info.see_more_info_on_x_locs/ }),
  newKeyInput: byRole('textbox'),
  updateInputButton: byRole('button', { name: 'update_verb' }),
  resetInputButton: byRole('button', { name: 'reset_verb' }),
};

afterEach(() => {
  componentsMock.reset();
});

it('can update project key', async () => {
  renderProjectInformationApp();
  expect(await ui.projectPageTitle.find()).toBeInTheDocument();
});

function renderProjectInformationApp() {
  return renderAppWithComponentContext(
    'project/information',
    routes,
    {},
    { component: componentsMock.components[0].component }
  );
}
