/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { ComponentProps, createContext, ReactNode, useContext } from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../helpers';
import { FCProps } from '../types/misc';

interface TableBaseProps extends ComponentProps<'table'> {
  header?: ReactNode;
  noHeaderTopBorder?: boolean;
  noSidePadding?: boolean;
}

interface GenericTableProps extends TableBaseProps {
  columnCount: number;
  gridTemplate?: never;
}

interface CustomTableProps extends TableBaseProps {
  columnCount?: never;
  gridTemplate: string;
}

export type TableProps = GenericTableProps | CustomTableProps;

export function Table(props: TableProps) {
  const { className, header, children, noHeaderTopBorder, noSidePadding, ...rest } = props;

  return (
    <StyledTable
      className={classNames(
        { 'no-header-top-border': noHeaderTopBorder, 'no-side-padding': noSidePadding },
        className
      )}
      {...rest}
    >
      {header && (
        <thead>
          <CellTypeContext.Provider value="th">{header}</CellTypeContext.Provider>
        </thead>
      )}
      <tbody>{children}</tbody>
    </StyledTable>
  );
}

export const TableRow = styled.tr`
  td,
  th {
    border-top: ${themeBorder('default')};
  }

  .no-header-top-border & th {
    ${tw`sw-border-t-0`}
  }

  td:first-of-type,
  th:first-of-type,
  td:last-child,
  th:last-child {
    border-right: ${themeBorder('default', 'transparent')};
    border-left: ${themeBorder('default', 'transparent')};
  }

  .no-side-padding & {
    td:first-of-type,
    th:first-of-type {
      ${tw`sw-pl-0`}
    }

    td:last-child,
    th:last-child {
      ${tw`sw-pr-0`}
    }
  }

  &:last-child > td {
    border-bottom: ${themeBorder('default')};
  }
`;

interface TableRowInteractiveProps extends FCProps<typeof TableRow> {
  selected?: boolean;
}

function TableRowInteractiveBase({
  className,
  children,
  selected,
  ...props
}: TableRowInteractiveProps) {
  return (
    <TableRow aria-selected={selected} className={classNames(className, { selected })} {...props}>
      {children}
    </TableRow>
  );
}

export const TableRowInteractive = styled(TableRowInteractiveBase)`
  &:hover > td,
  &.selected > td,
  &.selected > th,
  th.selected,
  td.selected {
    background: ${themeColor('tableRowHover')};
  }

  &.selected > td:first-of-type,
  &.selected > th:first-of-type,
  th.selected:first-of-type,
  td.selected:first-of-type {
    border-left: ${themeBorder('default', 'tableRowSelected')};
  }

  &.selected > td,
  &.selected > th,
  th.selected,
  td.selected {
    border-top: ${themeBorder('default', 'tableRowSelected')};
    border-bottom: ${themeBorder('default', 'tableRowSelected')};
  }

  &.selected > td:last-child,
  &.selected > th:last-child,
  th.selected:last-child,
  td.selected:last-child {
    border-right: ${themeBorder('default', 'tableRowSelected')};
  }

  &.selected + &:not(.selected) > td {
    border-top: none;
  }
`;

export const ContentCell = styled(CellComponent)`
  ${tw`sw-text-left sw-justify-start`}
`;
export const NumericalCell = styled(CellComponent)`
  ${tw`sw-text-right sw-justify-end`}
`;
export const RatingCell = styled(CellComponent)`
  ${tw`sw-text-right sw-justify-end`}
`;
export const CheckboxCell = styled(CellComponent)`
  ${tw`sw-text-center`}
  ${tw`sw-flex`}
  ${tw`sw-items-center sw-justify-center`}
`;

const StyledTable = styled.table<GenericTableProps | CustomTableProps>`
  display: grid;
  grid-template-columns: ${(props) => props.gridTemplate ?? `repeat(${props.columnCount}, 1fr)`};
  width: 100%;
  border-collapse: collapse;

  thead,
  tbody,
  tr {
    display: contents;
  }
`;

const CellComponentStyled = styled.td`
  color: ${themeColor('pageContent')};
  ${tw`sw-flex sw-items-center`}
  ${tw`sw-body-sm`}
  ${tw`sw-py-4 sw-px-2`}
  ${tw`sw-align-top`}

  thead > tr > & {
    color: ${themeColor('pageTitle')};

    ${tw`sw-body-sm-highlight`}
  }
`;

const CellTypeContext = createContext<'th' | 'td'>('td');

export function CellComponent(props: ComponentProps<'th' | 'td'>) {
  const containerType = useContext(CellTypeContext);
  return <CellComponentStyled as={containerType} {...props} />;
}
