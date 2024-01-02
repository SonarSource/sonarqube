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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import EmbedDocsPopupHelper from '../EmbedDocsPopupHelper';
import Suggestions from '../Suggestions';
import SuggestionsProvider from '../SuggestionsProvider';

it('should render with no suggestions', async () => {
  const user = userEvent.setup();
  renderEmbedDocsPopup();

  await user.click(screen.getByRole('button', { name: 'help' }));

  expect(screen.getByText('docs.documentation')).toBeInTheDocument();
  expect(screen.queryByText('docs.suggestion')).not.toBeInTheDocument();

  expect(screen.getByText('docs.documentation')).toHaveFocus();
});

it('should be able to render with suggestions and remove them', async () => {
  const user = userEvent.setup();
  renderEmbedDocsPopup();

  await user.click(screen.getByRole('button', { name: 'help' }));
  await user.click(screen.getByRole('button', { name: 'add.suggestion' }));

  await user.click(screen.getByRole('button', { name: 'help' }));

  expect(screen.getByText('docs.suggestion')).toBeInTheDocument();
  expect(screen.getByText('About Background Tasks')).toBeInTheDocument();

  expect(screen.getByText('About Background Tasks')).toHaveFocus();

  await user.click(screen.getByRole('button', { name: 'remove.suggestion' }));
  await user.click(screen.getByRole('button', { name: 'help' }));
  expect(screen.queryByText('docs.suggestion')).not.toBeInTheDocument();

  expect(screen.getByText('docs.documentation')).toHaveFocus();
});

function renderEmbedDocsPopup() {
  function Test() {
    const [suggestions, setSuggestions] = React.useState<string[]>(['account']);

    const addSuggestion = () => {
      setSuggestions([...suggestions, 'background_tasks']);
    };

    return (
      <SuggestionsProvider>
        <button onClick={addSuggestion} type="button">
          add.suggestion
        </button>
        <button
          onClick={() => {
            setSuggestions([]);
          }}
          type="button"
        >
          remove.suggestion
        </button>
        <EmbedDocsPopupHelper />
        {suggestions.map((suggestion) => (
          <Suggestions key={suggestion} suggestions={suggestion} />
        ))}
      </SuggestionsProvider>
    );
  }

  return renderComponent(<Test />);
}
