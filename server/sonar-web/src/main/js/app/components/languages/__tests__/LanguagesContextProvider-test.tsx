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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getLanguages } from '../../../../api/languages';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import LanguagesContextProvider from '../LanguagesContextProvider';

jest.mock('../../../../api/languages', () => ({
  getLanguages: jest.fn().mockResolvedValue({}),
}));

it('should call language', async () => {
  const languages = { c: { key: 'c', name: 'c' } };
  (getLanguages as jest.Mock).mockResolvedValueOnce(languages);
  const wrapper = shallowRender();

  expect(getLanguages).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(wrapper.state()).toEqual({ languages });
});

function shallowRender() {
  return shallow<LanguagesContextProvider>(
    <LanguagesContextProvider>
      <div />
    </LanguagesContextProvider>
  );
}
