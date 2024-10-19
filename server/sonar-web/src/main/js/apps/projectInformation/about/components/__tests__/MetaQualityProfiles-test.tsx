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
import { searchRules } from '../../../../../api/rules';
import { LanguagesContext } from '../../../../../app/components/languages/LanguagesContext';
import { mockLanguage, mockPaging, mockQualityProfile } from '../../../../../helpers/testMocks';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { SearchRulesResponse } from '../../../../../types/coding-rules';
import { Dict } from '../../../../../types/types';
import { MetaQualityProfiles } from '../MetaQualityProfiles';

jest.mock('../../../../../api/rules', () => {
  return {
    searchRules: jest.fn().mockResolvedValue({
      total: 10,
    }),
  };
});

it('should render correctly', async () => {
  const totals: Dict<number> = {
    js: 0,
    ts: 10,
    css: 0,
  };
  jest
    .mocked(searchRules)
    .mockImplementation(({ qprofile }: { qprofile: string }): Promise<SearchRulesResponse> => {
      return Promise.resolve({
        rules: [],
        paging: mockPaging({
          total: totals[qprofile],
        }),
      });
    });

  renderMetaQualityprofiles();

  expect(await screen.findByText('overview.deleted_profile.javascript')).toBeInTheDocument();
  expect(screen.getByText('overview.deprecated_profile.10')).toBeInTheDocument();
});

function renderMetaQualityprofiles(
  overrides: Partial<Parameters<typeof MetaQualityProfiles>[0]> = {},
) {
  return renderComponent(
    <LanguagesContext.Provider value={{ css: mockLanguage() }}>
      <MetaQualityProfiles
        profiles={[
          { ...mockQualityProfile({ key: 'js', name: 'javascript' }), deleted: true },
          { ...mockQualityProfile({ key: 'ts', name: 'typescript' }), deleted: false },
          {
            ...mockQualityProfile({
              key: 'css',
              name: 'style',
              language: 'css',
              languageName: 'CSS',
            }),
            deleted: false,
          },
        ]}
        {...overrides}
      />
    </LanguagesContext.Provider>,
  );
}
