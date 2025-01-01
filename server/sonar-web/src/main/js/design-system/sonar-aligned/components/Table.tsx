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

import styled from '@emotion/styled';
import classNames from 'classnames';
import { isNumber, times } from 'lodash';
import { ComponentProps, ReactNode, createContext, useContext } from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../../helpers/theme';
import { FCProps } from '../../types/misc';

interface TableBaseProps extends ComponentProps<'table'> {
  caption?: ReactNode;
  header?: ReactNode;
  noHeaderTopBorder?: boolean;
  noSidePadding?: boolean;
  withRoundedBorder?: boolean;
}

interface ColumnWidthsProps extends TableBaseProps {
  columnCount: number;
  columnWidths?: Array<number | string>;
  gridTemplate?: never;
}

interface GridTemplateProps extends TableBaseProps {
  columnCount?: never;
  columnWidths?: never;
  gridTemplate: string;
}

export type TableProps = ColumnWidthsProps | GridTemplateProps;

export function Table(props: Readonly<TableProps>) {
  const { columnCount, gridTemplate } = props;
  const {
    className,
    columnWidths,
    header,
    caption,
    children,
    noHeaderTopBorder,
    noSidePadding,
    withRoundedBorder,
    ...rest
  } = props;

  return (
    <StyledTable
      className={classNames(
        {
          'no-header-top-border': noHeaderTopBorder,
          'no-side-padding': noSidePadding,
          'with-rounded-border': withRoundedBorder,
          'with-grid-template': gridTemplate !== undefined,
        },
        className,
      )}
      {...rest}
    >
      {isNumber(columnCount) && (
        <colgroup>
          {times(columnCount, (i) => (
            <col key={i} width={columnWidths?.[i] ?? 'auto'} />
          ))}
        </colgroup>
      )}

      {caption && (
        <caption>
          <div className="sw-py-4 sw-text-middle sw-flex sw-justify-center sw-typo-semibold">
            {caption}
          </div>
        </caption>
      )}

      {header && (
        <thead>
          <CellTypeContext.Provider value="th">{header}</CellTypeContext.Provider>
        </thead>
      )}

      <tbody>{children}</tbody>
    </StyledTable>
  );
}

export const TableSeparator = styled.tr`
  ${tw`sw-h-4`}
  border-top: ${themeBorder('default')};
`;

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
}: Readonly<TableRowInteractiveProps>) {
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

const CellTypeContext = createContext<'th' | 'td'>('td');
type CellComponentProps = ComponentProps<'th' | 'td'>;

export function CellComponent(props: CellComponentProps) {
  const containerType = useContext(CellTypeContext);
  return <CellComponentStyled as={containerType} {...props} />;
}

export function ContentCell({
  children,
  cellClassName,
  className,
  ...props
}: CellComponentProps & { cellClassName?: string }) {
  return (
    <CellComponent className={cellClassName} {...props}>
      <div
        className={classNames('sw-text-left sw-justify-start sw-flex sw-items-center', className)}
      >
        {children}
      </div>
    </CellComponent>
  );
}

export function NumericalCell({ children, ...props }: CellComponentProps) {
  return (
    <CellComponent {...props}>
      <div className="sw-text-right sw-justify-end sw-flex sw-items-center">{children}</div>
    </CellComponent>
  );
}

export function RatingCell({ children, ...props }: CellComponentProps) {
  return (
    <CellComponent {...props}>
      <div className="sw-text-right sw-justify-end sw-flex sw-items-center">{children}</div>
    </CellComponent>
  );
}

export function ActionCell({ children, ...props }: CellComponentProps) {
  return (
    <CellComponent {...props}>
      <div className="sw-text-right sw-justify-end sw-flex sw-items-center">{children}</div>
    </CellComponent>
  );
}

export function CheckboxCell({ children, ...props }: CellComponentProps) {
  return (
    <CellComponent {...props}>
      <div className="sw-text-center sw-justify-center sw-flex sw-items-center">{children}</div>
    </CellComponent>
  );
}

const StyledTable = styled.table<{ gridTemplate?: string }>`
  width: 100%;
  border-collapse: collapse;

  &.with-grid-template {
    display: grid;
    grid-template-columns: ${(props) => props.gridTemplate};
    thead,
    tbody,
    tr {
      display: contents;
    }
  }

  &.with-rounded-border {
    border-collapse: separate;
    border: ${themeBorder('default', 'breakdownBorder')};
    ${tw`sw-rounded-1`};

    th:first-of-type {
      ${tw`sw-rounded-tl-1`};
    }
    th:last-of-type {
      ${tw`sw-rounded-tr-1`};
    }

    tr:last-child > td {
      border-bottom: none;
    }
  }
`;

const CellComponentStyled = styled.td`
  color: ${themeColor('pageContent')};
  ${tw`sw-items-center`}
  ${tw`sw-typo-default`}
  ${tw`sw-py-4 sw-px-2`}
  ${tw`sw-align-middle`}

  thead > tr > & {
    color: ${themeColor('pageTitle')};

    ${tw`sw-typo-semibold`}
  }
`;
