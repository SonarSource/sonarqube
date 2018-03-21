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
import { orderBy, without } from 'lodash';
import { formatFacetStat, Query } from '../utils';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import { translate } from '../../../helpers/l10n';

interface Props {
  facetMode: string;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  stats: { [x: string]: number } | undefined;
  types: string[];
}

export default class TypeFacet extends React.PureComponent<Props> {
  property = 'types';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue: string) => {
    const { types } = this.props;
    const newValue = orderBy(
      types.includes(itemValue) ? without(types, itemValue) : [...types, itemValue]
    );
    this.props.onChange({ [this.property]: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [] });
  };

  getStat(type: string) {
    const { stats } = this.props;
    return stats ? stats[type] : undefined;
  }

  renderItem = (type: string) => {
    const active = this.props.types.includes(type);
    const stat = this.getStat(type);

    return (
      <FacetItem
        active={active}
        disabled={stat === 0 && !active}
        key={type}
        name={
          <span>
            <IssueTypeIcon query={type} /> {translate('issue.type', type)}
          </span>
        }
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat, this.props.facetMode)}
        value={type}
      />
    );
  };

  render() {
    const types = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];
    const values = this.props.types.map(type => translate('issue.type', type));

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && <FacetItemsList>{types.map(this.renderItem)}</FacetItemsList>}
      </FacetBox>
    );
  }
}
