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
import classNames from 'classnames';
import { FacetBox, FacetItem } from 'design-system';
import { orderBy, sortBy, without } from 'lodash';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { MetricType } from '../../../types/metrics';
import { Dict } from '../../../types/types';
import { FacetItemsList } from '../../issues/sidebar/FacetItemsList';
import { MultipleSelectionHint } from '../../issues/sidebar/MultipleSelectionHint';
import { FacetKey } from '../query';

export interface BasicProps {
  onChange: (changes: Dict<string | string[] | undefined>) => void;
  onToggle: (facet: FacetKey) => void;
  open: boolean;
  stats?: Dict<number>;
  values: string[];
  help?: React.ReactNode;
}

interface Props extends BasicProps {
  disabled?: boolean;
  disabledHelper?: string;
  options?: string[];
  property: FacetKey;
  renderFooter?: () => React.ReactNode;
  renderName?: (value: string, disabled: boolean) => React.ReactNode;
  renderTextName?: (value: string) => string;
  singleSelection?: boolean;
}

export default class Facet extends React.PureComponent<Props> {
  handleItemClick = (itemValue: string, multiple: boolean) => {
    const { values } = this.props;
    let newValue;
    if (this.props.singleSelection) {
      const value = values.length ? values[0] : undefined;
      newValue = itemValue === value ? undefined : itemValue;
    } else if (multiple) {
      newValue = orderBy(
        values.includes(itemValue) ? without(values, itemValue) : [...values, itemValue],
      );
    } else {
      newValue = values.includes(itemValue) && values.length < 2 ? [] : [itemValue];
    }
    this.props.onChange({ [this.props.property]: newValue });
  };

  handleHeaderClick = () => this.props.onToggle(this.props.property);

  handleClear = () => this.props.onChange({ [this.props.property]: [] });

  getStat = (value: string) => this.props.stats && this.props.stats[value];

  renderItem = (value: string) => {
    const active = this.props.values.includes(value);
    const stat = this.getStat(value);
    const disabled = stat === 0 || typeof stat === 'undefined';
    const { renderName = defaultRenderName, renderTextName = defaultRenderName } = this.props;

    return (
      <FacetItem
        className="it__search-navigator-facet"
        active={active}
        key={value}
        name={renderName(value, disabled)}
        onClick={this.handleItemClick}
        stat={stat && formatMeasure(stat, MetricType.ShortInteger)}
        value={value}
        tooltip={renderTextName(value)}
      />
    );
  };

  render() {
    const {
      disabled,
      disabledHelper,
      open,
      property,
      renderTextName = defaultRenderName,
      stats,
      help,
      values,
    } = this.props;
    const items =
      this.props.options ||
      (stats &&
        sortBy(
          Object.keys(stats),
          (key) => -stats[key],
          (key) => renderTextName(key).toLowerCase(),
        ));
    const headerId = `facet_${property}`;
    const nbSelectableItems =
      items?.filter((item) => (stats ? stats[item] : undefined)).length ?? 0;
    const nbSelectedItems = values.length;

    return (
      <FacetBox
        className={classNames('it__search-navigator-facet-box', {
          'it__search-navigator-facet-box-forbidden': disabled,
        })}
        data-property={property}
        clearIconLabel={translate('clear')}
        count={values.length}
        id={headerId}
        name={translate('coding_rules.facet', property)}
        onClear={this.handleClear}
        onClick={disabled ? undefined : this.handleHeaderClick}
        open={open && !disabled}
        disabled={disabled}
        disabledHelper={disabledHelper}
        tooltipComponent={Tooltip}
        help={help}
      >
        {open && items !== undefined && (
          <FacetItemsList labelledby={headerId}>{items.map(this.renderItem)}</FacetItemsList>
        )}

        {open && this.props.renderFooter !== undefined && this.props.renderFooter()}

        <MultipleSelectionHint
          nbSelectableItems={nbSelectableItems}
          nbSelectedItems={nbSelectedItems}
        />
      </FacetBox>
    );
  }
}

function defaultRenderName(value: string) {
  return value;
}
