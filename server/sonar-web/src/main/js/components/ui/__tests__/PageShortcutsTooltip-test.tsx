/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import PageShortcutsTooltip, { PageShortcutsTooltipProps } from '../PageShortcutsTooltip';

const leftAndRightLabel = 'left & right';
const leftLabel = 'left';
const upAndDownLabel = 'up & down';
const metaModifierLabel = 'meta';

it('should render all the labels', async () => {
  const user = userEvent.setup();

  renderPageShortcutsTooltip({
    leftAndRightLabel,
    leftLabel,
    upAndDownLabel,
    metaModifierLabel,
  });

  await user.hover(screen.getByText('←'));

  expect(await screen.findByText(leftAndRightLabel)).toBeInTheDocument();
  expect(screen.getByText(leftLabel)).toBeInTheDocument();
  expect(screen.getByText(upAndDownLabel)).toBeInTheDocument();
  expect(screen.getByText(metaModifierLabel)).toBeInTheDocument();
});

it('should render left & right labels without up&down', async () => {
  const user = userEvent.setup();

  renderPageShortcutsTooltip({
    leftAndRightLabel,
    leftLabel,
  });

  await user.hover(screen.getByText('←'));

  expect(await screen.findByText(leftAndRightLabel)).toBeInTheDocument();
  expect(screen.getByText(leftLabel)).toBeInTheDocument();
});

function renderPageShortcutsTooltip(props: Partial<PageShortcutsTooltipProps> = {}) {
  return renderComponent(<PageShortcutsTooltip {...props} />);
}
