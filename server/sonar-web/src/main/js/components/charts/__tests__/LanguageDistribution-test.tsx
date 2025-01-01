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

import { renderComponent } from '../../../helpers/testReactTestingUtils';
import LanguageDistribution, { LanguageDistributionProps } from '../LanguageDistribution';

it('should render correctly', () => {
  const { container } = renderLanguageDistribution();
  expect(container).toMatchSnapshot();
});

function renderLanguageDistribution(props: Partial<LanguageDistributionProps> = {}) {
  return renderComponent(
    <LanguageDistribution
      distribution="java=1734;js=845;cpp=73;<null>=15"
      languages={{ java: { key: 'java', name: 'Java' }, js: { key: 'js', name: 'JavaScript' } }}
      {...props}
    />,
  );
}
