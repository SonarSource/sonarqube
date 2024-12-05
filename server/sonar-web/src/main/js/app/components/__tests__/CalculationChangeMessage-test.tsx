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

import { Outlet, Route } from 'react-router-dom';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { ModeServiceMock } from '../../../api/mocks/ModeServiceMock';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { Mode } from '../../../types/mode';
import CalculationChangeMessage from '../calculation-notification/CalculationChangeMessage';

const ui = {
  alert: byRole('alert'),
  learnMoreLink: byRole('link', {
    name: 'notification.calculation_change.message_link open_in_new_tab',
  }),

  alertText: byText('notification.calculation_change.message'),
};

const modeHandler = new ModeServiceMock();

beforeEach(() => {
  modeHandler.reset();
});

it.each([
  ['Project', '/projects'],
  ['Portfolios', '/portfolios'],
])('should render on %s page', (_, path) => {
  render(path);
  expect(ui.alert.get()).toBeInTheDocument();
  expect(ui.alertText.get()).toBeInTheDocument();
  expect(ui.learnMoreLink.get()).toBeInTheDocument();
});

it.each([
  ['Project', '/projects'],
  ['Portfolios', '/portfolios'],
])('should not render on %s page if isStandardMode', (_, path) => {
  modeHandler.setMode(Mode.Standard);
  render(path);
  expect(ui.alert.get()).toBeInTheDocument();
  expect(ui.alertText.get()).toBeInTheDocument();
  expect(ui.learnMoreLink.get()).toBeInTheDocument();
});

it('should not render on other page', () => {
  render('/other');
  expect(ui.alert.query()).not.toBeInTheDocument();
  expect(ui.alertText.query()).not.toBeInTheDocument();
  expect(ui.learnMoreLink.query()).not.toBeInTheDocument();
});

function render(indexPath = '/projects') {
  renderAppRoutes(indexPath, () => (
    <Route
      path="/"
      element={
        <>
          <CalculationChangeMessage />
          <Outlet />
        </>
      }
    >
      <Route path="projects" element={<div>Projects</div>} />
      <Route path="portfolios" element={<div>Portfolios</div>} />
      <Route path="other" element={<div>Other page</div>} />
    </Route>
  ));
}
