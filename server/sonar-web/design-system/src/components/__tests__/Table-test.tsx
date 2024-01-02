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
import { render } from '../../helpers/testUtils';
import {
  CheckboxCell,
  ContentCell,
  NumericalCell,
  Table,
  TableProps,
  TableRow,
  TableRowInteractive,
} from '../Table';

it('check that the html structure and style is correct for a regular table', () => {
  renderTable({
    columnCount: 3,
    'aria-colcount': 3,
    header: (
      <TableRow>
        <ContentCell>ContentCellHeader</ContentCell>
        <NumericalCell>NumericalCellHeader</NumericalCell>
        <CheckboxCell>CheckboxCellHeader</CheckboxCell>
      </TableRow>
    ),
    children: (
      <>
        <TableRowInteractive>
          <ContentCell>ContentCell 1</ContentCell>
          <NumericalCell>NumericalCell 1</NumericalCell>
          <CheckboxCell>CheckboxCell 1</CheckboxCell>
        </TableRowInteractive>
        <TableRowInteractive selected>
          <ContentCell>ContentCell 2</ContentCell>
          <NumericalCell>NumericalCell 2</NumericalCell>
          <CheckboxCell>CheckboxCell 2</CheckboxCell>
        </TableRowInteractive>
        <TableRow>
          <ContentCell aria-colspan={3}>ContentCell 3</ContentCell>
        </TableRow>
        <TableRowInteractive>
          <NumericalCell aria-colindex={2}>NumericalCell 4</NumericalCell>
          <CheckboxCell aria-colindex={3}>CheckboxCell 4</CheckboxCell>
        </TableRowInteractive>
      </>
    ),
  });

  // Table should have accessible attribute
  expect(screen.getByRole('table')).toHaveAttribute('aria-colcount', '3');

  // Rows should have accessible attributes
  expect(
    screen.getByRole('row', { name: 'ContentCellHeader NumericalCellHeader CheckboxCellHeader' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('row', {
      name: 'ContentCell 1 NumericalCell 1 CheckboxCell 1',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('row', {
      name: 'ContentCell 1 NumericalCell 1 CheckboxCell 1',
    }),
  ).not.toHaveAttribute('aria-selected');
  expect(
    screen.getByRole('row', {
      selected: true,
      name: 'ContentCell 2 NumericalCell 2 CheckboxCell 2',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('row', {
      name: 'NumericalCell 4 CheckboxCell 4',
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
});

function renderTable(props: TableProps) {
  return render(<Table {...props}>{props.children}</Table>);
}
