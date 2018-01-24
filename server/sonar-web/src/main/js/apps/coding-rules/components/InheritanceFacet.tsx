/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import * as classNames from 'classnames';
import { Query, FacetKey } from '../query';
import { RuleInheritance } from '../../../app/types';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import { translate } from '../../../helpers/l10n';

interface Props {
  disabled: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (facet: FacetKey) => void;
  open: boolean;
  value: RuleInheritance | undefined;
}

export default class InheritanceFacet extends React.PureComponent<Props> {
  handleItemClick = (selected: RuleInheritance) => {
    const { value } = this.props;
    const newValue = selected === value ? undefined : selected;
    this.props.onChange({ inheritance: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle('inheritance');
  };

  handleClear = () => {
    this.props.onChange({ inheritance: undefined });
  };

  renderName = (value: RuleInheritance) => {
    return translate('coding_rules.filters.inheritance', value.toLowerCase());
  };

  renderItem = (value: RuleInheritance) => {
    const active = this.props.value === value;

    return (
      <FacetItem
        active={active}
        key={String(value)}
        name={this.renderName(value)}
        onClick={this.handleItemClick}
        value={String(value)}
      />
    );
  };

  render() {
    const values = [];
    if (this.props.value !== undefined) {
      values.push(this.renderName(this.props.value));
    }

    const items = Object.values(RuleInheritance);

    return (
      <FacetBox
        className={classNames({ 'search-navigator-facet-box-forbidden': this.props.disabled })}>
        <FacetHeader
          helper={
            this.props.disabled ? translate('coding_rules.filters.inheritance.inactive') : undefined
          }
          name={translate('coding_rules.facet.inheritance')}
          onClear={this.handleClear}
          onClick={this.props.disabled ? undefined : this.handleHeaderClick}
          open={this.props.open && !this.props.disabled}
          values={values}
        />

        {this.props.open &&
          !this.props.disabled && <FacetItemsList>{items.map(this.renderItem)}</FacetItemsList>}
      </FacetBox>
    );
  }
}
