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
import { screen } from '@testing-library/react';
import * as React from 'react';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ProjectCardLanguages } from '../ProjectCardLanguages';

const languages = {
  java: { key: 'java', name: 'Java' },
  js: { key: 'js', name: 'JavaScript' },
};

it('renders', () => {
  renderProjectCardLanguages('java=137;js=15');
  expect(screen.getByText('Java, JavaScript')).toBeInTheDocument();
});

it('sorts languages', () => {
  renderProjectCardLanguages('java=13;js=152');
  expect(screen.getByText('JavaScript, Java')).toBeInTheDocument();
});

it('handles unknown languages', () => {
  renderProjectCardLanguages('java=13;cpp=18');
  expect(screen.getByText('cpp, Java')).toBeInTheDocument();
});

function renderProjectCardLanguages(distribution?: string) {
  renderComponent(<ProjectCardLanguages languages={languages} distribution={distribution} />);
}
