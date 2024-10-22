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
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import { LanguagesFilter } from '../LanguagesFilter';

it('renders language names', () => {
  renderLanguagesFilter();
  expect(screen.getByText('Javascript')).toBeInTheDocument();
  expect(screen.getByText('ts')).toBeInTheDocument();
  expect(screen.getByText('Java')).toBeInTheDocument();
  expect(screen.getByText('xml')).toBeInTheDocument();
  expect(screen.getByText('unknown')).toBeInTheDocument();
});

it('filters options', async () => {
  const user = userEvent.setup();

  renderLanguagesFilter();

  await user.click(screen.getByLabelText('search_verb'));

  await user.keyboard('ja');

  expect(screen.getByTitle('Javascript')).toBeInTheDocument();
  expect(screen.queryByTitle('ts')).not.toBeInTheDocument();
  expect(screen.getByTitle('Java')).toBeInTheDocument();
  expect(screen.queryByTitle('xml')).not.toBeInTheDocument();
  expect(screen.queryByTitle('unknown')).not.toBeInTheDocument();
});

it('updates the filter query', async () => {
  const user = userEvent.setup();

  const onQueryChange = jest.fn();

  renderLanguagesFilter({ onQueryChange });

  await user.click(screen.getByText('Java'));

  expect(onQueryChange).toHaveBeenCalledWith({ languages: 'java' });
});

function renderLanguagesFilter(props: Partial<ComponentPropsType<typeof LanguagesFilter>> = {}) {
  renderComponent(
    <LanguagesFilter
      languages={{
        js: { name: 'Javascript', key: 'js' },
        java: { name: 'Java', key: 'java' },
        xml: { name: '', key: 'xml' },
      }}
      loadSearchResultCount={jest.fn()}
      onQueryChange={jest.fn()}
      query={{}}
      facet={{ js: 12, ts: 7, java: 4, xml: 1, '<null>': 1 }}
      value={['js', 'ts']}
      {...props}
    />,
  );
}
