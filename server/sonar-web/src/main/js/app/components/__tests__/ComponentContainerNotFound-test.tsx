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
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import ComponentContainerNotFound from '../ComponentContainerNotFound';

it('should render portfolio 404 correctly', () => {
  renderComponentContainerNotFound(true);

  expect(screen.getByText('dashboard.portfolio.not_found')).toBeInTheDocument();
  expect(screen.getByText('dashboard.portfolio.not_found.2')).toBeInTheDocument();
});

it('should render project 404 correctly', () => {
  renderComponentContainerNotFound(false);

  expect(screen.getByText('dashboard.project.not_found')).toBeInTheDocument();
  expect(screen.getByText('dashboard.project.not_found.2')).toBeInTheDocument();
});

function renderComponentContainerNotFound(isPortfolioLike: boolean) {
  return renderComponent(<ComponentContainerNotFound isPortfolioLike={isPortfolioLike} />);
}
