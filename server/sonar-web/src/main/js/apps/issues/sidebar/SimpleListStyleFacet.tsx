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
import { FacetBox, FacetItem } from 'design-system';
import { without } from 'lodash';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Query, formatFacetStat } from '../utils';
import { FacetItemsList } from './FacetItemsList';
import { MultipleSelectionHint } from './MultipleSelectionHint';

export interface CommonProps {
  fetching: boolean;
  needIssueSync?: boolean;
  help?: React.ReactNode;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  stats: Dict<number> | undefined;
}

interface Props<T = string> extends CommonProps {
  property: string;
  listItems: Array<T>;
  itemNamePrefix: string;
  selectedItems: Array<T>;
  renderIcon?: (item: string, disabled: boolean) => React.ReactNode;
}

export function SimpleListStyleFacet(props: Props) {
  const {
    fetching,
    open,
    selectedItems = [],
    stats = {},
    needIssueSync,
    property,
    listItems,
    itemNamePrefix,
    help,
    renderIcon,
  } = props;

  const nbSelectableItems = listItems.filter((item) => stats[item]).length;
  const nbSelectedItems = selectedItems.length;
  const headerId = `facet_${property}`;

  return (
    <FacetBox
      className="it__search-navigator-facet-box it__search-navigator-facet-header"
      clearIconLabel={translate('clear')}
      count={nbSelectedItems}
      countLabel={translateWithParameters('x_selected', nbSelectedItems)}
      data-property={property}
      id={headerId}
      loading={fetching}
      name={translate('issues.facet', property)}
      onClear={() => props.onChange({ [property]: [] })}
      onClick={() => props.onToggle(property)}
      open={open}
      help={help}
    >
      <FacetItemsList labelledby={headerId}>
        {listItems.map((item) => {
          const active = selectedItems.includes(item);
          const stat = stats[item];
          const disabled = stat === 0 || typeof stat === 'undefined';

          return (
            <FacetItem
              active={active}
              className="it__search-navigator-facet"
              key={item}
              icon={renderIcon?.(item, disabled)}
              name={translate(itemNamePrefix, item)}
              onClick={(itemValue, multiple) => {
                if (multiple) {
                  props.onChange({
                    [property]: active
                      ? without(selectedItems, itemValue)
                      : [...selectedItems, itemValue],
                  });
                } else {
                  props.onChange({
                    [property]: active && selectedItems.length === 1 ? [] : [itemValue],
                  });
                }
              }}
              stat={(!needIssueSync && formatFacetStat(stat)) ?? 0}
              value={item}
            />
          );
        })}
      </FacetItemsList>

      <MultipleSelectionHint
        nbSelectableItems={nbSelectableItems}
        nbSelectedItems={nbSelectedItems}
      />
    </FacetBox>
  );
}
