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

import * as React from 'react';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { getLanguages } from '../../../../api/languages';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Language } from '../../../../types/languages';
import { LanguagesContext } from '../LanguagesContext';
import LanguagesContextProvider from '../LanguagesContextProvider';

jest.mock('../../../../api/languages', () => ({
  getLanguages: jest.fn().mockResolvedValue([]),
}));

it('should call language', async () => {
  const languages: Language[] = [
    { key: 'c', name: 'c' },
    { key: 'js', name: 'Javascript' },
  ];
  jest.mocked(getLanguages).mockResolvedValueOnce(languages);
  renderLanguagesContextProvider();

  expect(await byRole('listitem').findAll()).toHaveLength(2);
});

function renderLanguagesContextProvider() {
  return renderComponent(
    <LanguagesContextProvider>
      <Consumer />
    </LanguagesContextProvider>,
  );
}

function Consumer() {
  const languages = React.useContext(LanguagesContext);
  return (
    <ul>
      {Object.keys(languages).map((k) => (
        <li key={k}>{languages[k].name}</li>
      ))}
    </ul>
  );
}
