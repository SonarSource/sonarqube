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
import styled from '@emotion/styled';
import * as React from 'react';
import ReactSelect, { GroupTypeBase, IndicatorProps, Props, StylesConfig } from 'react-select';
import { colors, others, sizes, zIndexes } from '../../app/theme';

const ArrowSpan = styled.span`
  border-color: #999 transparent transparent;
  border-style: solid;
  border-width: 4px 4px 2px;
  display: inline-block;
  height: 0;
  width: 0;
`;

export default function Select<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupTypeBase<Option> = GroupTypeBase<Option>
>(props: Props<Option, IsMulti, Group>) {
  function DropdownIndicator({ innerProps }: IndicatorProps<Option, IsMulti, Group>) {
    return <ArrowSpan {...innerProps} />;
  }

  return (
    <ReactSelect
      {...props}
      styles={selectStyle<Option, IsMulti, Group>()}
      components={{
        ...props.components,
        DropdownIndicator
      }}
    />
  );
}

export function selectStyle<
  Option,
  IsMulti extends boolean,
  Group extends GroupTypeBase<Option>
>(): StylesConfig<Option, IsMulti, Group> {
  return {
    container: () => ({
      position: 'relative',
      display: 'inline-block',
      verticalAlign: 'middle',
      fontSize: '12px',
      textAlign: 'left'
    }),
    control: () => ({
      position: 'relative',
      display: 'table',
      width: '100%',
      height: `${sizes.controlHeight}`,
      lineHeight: `calc(${sizes.controlHeight} - 2px)`,
      border: `1px solid ${colors.gray80}`,
      borderCollapse: 'separate',
      borderRadius: '2px',
      backgroundColor: '#fff',
      boxSizing: 'border-box',
      color: `${colors.baseFontColor}`,
      cursor: 'default',
      outline: 'none'
    }),
    singleValue: () => ({
      bottom: 0,
      left: 0,
      lineHeight: '23px',
      paddingLeft: '8px',
      paddingRight: '24px',
      position: 'absolute',
      right: 0,
      top: 0,
      maxWidth: '100%',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap'
    }),
    valueContainer: () => ({
      bottom: 0,
      left: 0,
      lineHeight: '23px',
      paddingLeft: '8px',
      paddingRight: '24px',
      position: 'absolute',
      right: 0,
      top: 0,
      maxWidth: '100%',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
      whiteSpace: 'nowrap'
    }),
    indicatorsContainer: () => ({
      cursor: 'pointer',
      display: 'table-cell',
      position: 'relative',
      textAlign: 'center',
      verticalAlign: 'middle',
      width: '20px',
      paddingRight: '5px'
    }),
    menu: () => ({
      borderBottomRightRadius: '4px',
      borderBottomLeftRadius: '4px',
      backgroundColor: '#fff',
      border: '1px solid #ccc',
      borderTopColor: `${colors.barBorderColor}`,
      boxSizing: 'border-box',
      marginTop: '-1px',
      maxHeight: '200px',
      position: 'absolute',
      top: '100%',
      width: '100%',
      zIndex: `${zIndexes.dropdownMenuZIndex}`,
      webkitOverflowScrolling: 'touch',
      boxShadow: `${others.defaultShadow}`
    }),
    menuList: () => ({
      maxHeight: '198px',
      padding: '5px 0',
      overflowY: 'auto'
    }),
    option: (_provided, state) => ({
      display: 'block',
      lineHeight: '20px',
      padding: '0 8px',
      boxSizing: 'border-box',
      color: `${colors.baseFontColor}`,
      backgroundColor: state.isFocused ? `${colors.barBackgroundColor}` : 'white',
      fontSize: `${sizes.smallFontSize}`,
      cursor: 'pointer',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis'
    })
  };
}
