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
import { Query, FacetKey } from '../query';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import { translate } from '../../../helpers/l10n';

interface Props {
  onChange: (changes: Partial<Query>) => void;
  onToggle: (facet: FacetKey) => void;
  open: boolean;
  value: boolean | undefined;
}

export default class TemplateFacet extends React.PureComponent<Props> {
  handleItemClick = (template: string) => {
    const { value } = this.props;
    const selectedValue = template === 'true';
    const newValue = value === selectedValue ? undefined : selectedValue;
    this.props.onChange({ template: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle('template');
  };

  handleClear = () => {
    this.props.onChange({ template: undefined });
  };

  renderName = (template: boolean) => {
    return template
      ? translate('coding_rules.filters.template.is_template')
      : translate('coding_rules.filters.template.is_not_template');
  };

  renderItem = (value: boolean) => {
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

    const items = [true, false];

    return (
      <FacetBox>
        <FacetHeader
          name={translate('coding_rules.facet.is_template')}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open &&
          items !== undefined && <FacetItemsList>{items.map(this.renderItem)}</FacetItemsList>}
      </FacetBox>
    );
  }
}
