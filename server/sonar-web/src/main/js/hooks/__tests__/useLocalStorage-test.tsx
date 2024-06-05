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
import React from 'react';
import { renderComponent } from '../../helpers/testReactTestingUtils';
import { FCProps } from '../../types/misc';
import useLocalStorage from '../useLocalStorage';

describe('useLocalStorage hook', () => {
  it('gets/sets boolean value', async () => {
    const user = userEvent.setup();
    renderLSComponent();

    expect(screen.getByRole('button', { name: 'show' })).toBeInTheDocument();
    user.click(screen.getByRole('button', { name: 'show' }));
    expect(await screen.findByText('text')).toBeInTheDocument();
  });

  it('gets/sets string value', async () => {
    const user = userEvent.setup();
    const props = { condition: (value: string) => value === 'ok', valueToSet: 'wow' };
    const { rerender } = renderLSComponent(props);

    expect(screen.getByRole('button', { name: 'show' })).toBeInTheDocument();
    user.click(screen.getByRole('button', { name: 'show' }));
    expect(screen.queryByText('text')).not.toBeInTheDocument();

    rerender(<LSComponent lsKey="test_ls" {...props} valueToSet="ok" />);
    user.click(screen.getByRole('button', { name: 'show' }));
    expect(await screen.findByText('text')).toBeInTheDocument();
  });
});

function renderLSComponent(props: Partial<FCProps<typeof LSComponent>> = {}) {
  return renderComponent(
    <LSComponent lsKey="test_ls" valueToSet condition={(value) => Boolean(value)} {...props} />,
  );
}

function LSComponent({
  lsKey,
  condition,
  initialValue,
  valueToSet,
}: Readonly<{
  condition: (value: boolean | string) => boolean;
  initialValue?: boolean | string;
  lsKey: string;
  valueToSet: boolean | string;
}>) {
  const [value, setValue] = useLocalStorage(lsKey, initialValue);

  return (
    <div>
      <button type="button" onClick={() => setValue(valueToSet)}>
        show
      </button>
      {condition(value) && <span>text</span>}
    </div>
  );
}
