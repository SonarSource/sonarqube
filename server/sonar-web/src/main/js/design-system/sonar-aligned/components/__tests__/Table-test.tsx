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
import { render } from '../../../helpers/testUtils';
import {
  ActionCell,
  CheckboxCell,
  ContentCell,
  NumericalCell,
  RatingCell,
  Table,
  TableProps,
  TableRow,
  TableRowInteractive,
} from '../Table';

it.each([
  [
    'using column count and widths',
    {
      columnCount: 5,
      columnWidths: ['1%', 'auto', '1%', '1%', '1%'],
      'aria-colcount': 5,
    },
  ],
  [
    'using column count only',
    {
      columnCount: 5,
      'aria-colcount': 5,
    },
  ],
  [
    'using column grid template only',
    {
      gridTemplate: '1fr auto 1fr 1fr 1fr',
      'aria-colcount': 5,
    },
  ],
])('check that the html structure and style is correct %s', (_, props) => {
  renderTable({
    ...props,
    header: (
      <TableRow>
        <ContentCell>ContentCellHeader</ContentCell>
        <NumericalCell>NumericalCellHeader</NumericalCell>
        <CheckboxCell>CheckboxCellHeader</CheckboxCell>
        <RatingCell>RatingCellHeader</RatingCell>
        <ActionCell>ActionCellHeader</ActionCell>
      </TableRow>
    ),
    children: (
      <>
        <TableRowInteractive>
          <ContentCell>ContentCell 1</ContentCell>
          <NumericalCell>NumericalCell 1</NumericalCell>
          <CheckboxCell>CheckboxCell 1</CheckboxCell>
          <RatingCell>RatingCell 1</RatingCell>
          <ActionCell>ActionCell 1</ActionCell>
        </TableRowInteractive>
        <TableRowInteractive selected>
          <ContentCell>ContentCell 2</ContentCell>
          <NumericalCell>NumericalCell 2</NumericalCell>
          <CheckboxCell>CheckboxCell 2</CheckboxCell>
          <RatingCell>RatingCell 2</RatingCell>
          <ActionCell>ActionCell 2</ActionCell>
        </TableRowInteractive>
        <TableRow>
          <ContentCell aria-colspan={5}>ContentCell 3</ContentCell>
        </TableRow>
        <TableRowInteractive>
          <NumericalCell aria-colindex={2}>NumericalCell 4</NumericalCell>
          <CheckboxCell aria-colindex={3}>CheckboxCell 4</CheckboxCell>
          <RatingCell aria-colindex={4}>RatingCell 4</RatingCell>
          <ActionCell aria-colindex={5}>ActionCell 4</ActionCell>
        </TableRowInteractive>
      </>
    ),
  });

  // Table should have accessible attribute
  expect(screen.getByRole('table')).toHaveAttribute('aria-colcount', '5');

  // Rows should have accessible attributes
  expect(
    screen.getByRole('row', {
      name: 'ContentCellHeader NumericalCellHeader CheckboxCellHeader RatingCellHeader ActionCellHeader',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('row', {
      name: 'ContentCell 1 NumericalCell 1 CheckboxCell 1 RatingCell 1 ActionCell 1',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('row', {
      name: 'ContentCell 1 NumericalCell 1 CheckboxCell 1 RatingCell 1 ActionCell 1',
    }),
  ).not.toHaveAttribute('aria-selected');
  expect(
    screen.getByRole('row', {
      selected: true,
      name: 'ContentCell 2 NumericalCell 2 CheckboxCell 2 RatingCell 2 ActionCell 2',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('row', {
      name: 'NumericalCell 4 CheckboxCell 4 RatingCell 4 ActionCell 4',
    }),
  ).toBeInTheDocument();

  // Cells should have accessible attributes
  expect(screen.getByRole('cell', { name: 'NumericalCell 4' })).toHaveAttribute(
    'aria-colindex',
    '2',
  );
  expect(screen.getByRole('cell', { name: 'CheckboxCell 4' })).toHaveAttribute(
    'aria-colindex',
    '3',
  );
  expect(screen.getByRole('cell', { name: 'RatingCell 4' })).toHaveAttribute('aria-colindex', '4');
  expect(screen.getByRole('cell', { name: 'ActionCell 4' })).toHaveAttribute('aria-colindex', '5');
});

function renderTable(props: TableProps) {
  return render(<Table {...props}>{props.children}</Table>);
}
