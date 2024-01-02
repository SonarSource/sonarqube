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
import * as React from 'react';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ProjectCardLanguages } from '../ProjectCardLanguages';

const languages = {
  java: { key: 'java', name: 'Java' },
  js: { key: 'js', name: 'JavaScript' },
};

it('should render normally', () => {
  renderProjectCardLanguages('java=137;js=15');
  expect(screen.getByText('Java, JavaScript')).toBeInTheDocument();
});

it('shoould sorts languages', () => {
  renderProjectCardLanguages('java=13;js=152');
  expect(screen.getByText('JavaScript, Java')).toBeInTheDocument();
});

it('should handle unknown languages', () => {
  renderProjectCardLanguages('java=13;cpp=18');
  expect(screen.getByText('cpp, Java')).toBeInTheDocument();
});

it('should handle more then 3 languages', async () => {
  renderProjectCardLanguages('java=137;js=18;cpp=10;c=8;php=4');
  await expect(screen.getByText('Java, JavaScript, ...')).toHaveATooltipWithContent(
    'JavaJavaScriptcppcphp',
  );
});

function renderProjectCardLanguages(distribution?: string) {
  return renderComponent(
    <ProjectCardLanguages languages={languages} distribution={distribution} />,
  );
}
