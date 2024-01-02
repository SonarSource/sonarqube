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
import { orderBy, without } from 'lodash';
import * as React from 'react';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import { ISSUE_TYPES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { formatFacetStat, Query } from '../utils';

interface Props {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  stats: Dict<number> | undefined;
  types: string[];
}

export default class TypeFacet extends React.PureComponent<Props> {
  property = 'types';

  static defaultProps = {
    open: true,
  };

  handleItemClick = (itemValue: string, multiple: boolean) => {
    const { types } = this.props;
    if (multiple) {
      const newValue = orderBy(
        types.includes(itemValue) ? without(types, itemValue) : [...types, itemValue]
      );
      this.props.onChange({ [this.property]: newValue });
    } else {
      this.props.onChange({
        [this.property]: types.includes(itemValue) && types.length < 2 ? [] : [itemValue],
      });
    }
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

  isFacetItemActive(type: string) {
    return this.props.types.includes(type);
  }

  renderItem = (type: string) => {
    const active = this.isFacetItemActive(type);
    const stat = this.getStat(type);

    return (
      <FacetItem
        active={active}
        key={type}
        name={
          <span className="display-flex-center">
            <IssueTypeIcon className="little-spacer-right" query={type} />{' '}
            {translate('issue.type', type)}
          </span>
        }
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat)}
        value={type}
      />
    );
  };

  render() {
    const { types, stats = {} } = this.props;
    const values = types.map((type) => translate('issue.type', type));

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          fetching={this.props.fetching}
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && (
          <>
            <FacetItemsList>
              {ISSUE_TYPES.filter((t) => t !== 'SECURITY_HOTSPOT').map(this.renderItem)}
            </FacetItemsList>
            <MultipleSelectionHint options={Object.keys(stats).length} values={types.length} />
          </>
        )}
      </FacetBox>
    );
  }
}
