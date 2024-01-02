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
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { SuggestionLink } from '../../../types/types';
import EmbedDocsPopup from '../EmbedDocsPopup';
import { SuggestionsContext } from '../SuggestionsContext';

it('should render with no suggestions', () => {
  renderEmbedDocsPopup();

  expect(screen.queryByText(suggestions[0].text)).not.toBeInTheDocument();
  expect(screen.getByText('docs.documentation')).toHaveFocus();
});

it('should render with suggestions', () => {
  renderEmbedDocsPopup(suggestions);

  suggestions.forEach((suggestion) => {
    expect(screen.getByText(suggestion.text)).toBeInTheDocument();
  });
  expect(screen.getByText(suggestions[0].text)).toHaveFocus();
});

const suggestions = [
  { link: '/docs/awesome-doc', text: 'mindblowing' },
  { link: '/docs/whocares', text: 'boring' },
];

function renderEmbedDocsPopup(suggestions: SuggestionLink[] = []) {
  return renderComponent(
    <SuggestionsContext.Provider
      value={{ addSuggestions: jest.fn(), removeSuggestions: jest.fn(), suggestions }}
    >
      <EmbedDocsPopup onClose={jest.fn()} />
    </SuggestionsContext.Provider>
  );
}
