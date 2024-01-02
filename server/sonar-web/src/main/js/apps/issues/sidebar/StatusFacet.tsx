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
import StatusHelper from '../../../components/shared/StatusHelper';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { formatFacetStat, Query } from '../utils';

interface Props {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  stats: Dict<number> | undefined;
  statuses: string[];
}

const STATUSES = ['OPEN', 'CONFIRMED', 'REOPENED', 'RESOLVED', 'CLOSED'];

export default class StatusFacet extends React.PureComponent<Props> {
  property = 'statuses';

  static defaultProps = { open: true };

  handleItemClick = (itemValue: string, multiple: boolean) => {
    const { statuses } = this.props;
    if (multiple) {
      const newValue = orderBy(
        statuses.includes(itemValue) ? without(statuses, itemValue) : [...statuses, itemValue]
      );
      this.props.onChange({ [this.property]: newValue });
    } else {
      this.props.onChange({
        [this.property]: statuses.includes(itemValue) && statuses.length < 2 ? [] : [itemValue],
      });
    }
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [] });
  };

  getStat(status: string) {
    const { stats } = this.props;
    return stats ? stats[status] : undefined;
  }

  renderItem = (status: string) => {
    const active = this.props.statuses.includes(status);
    const stat = this.getStat(status);

    return (
      <FacetItem
        active={active}
        halfWidth={true}
        key={status}
        name={<StatusHelper resolution={undefined} status={status} />}
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat)}
        tooltip={translate('issue.status', status)}
        value={status}
      />
    );
  };

  render() {
    const { statuses, stats = {} } = this.props;
    const values = statuses.map((status) => translate('issue.status', status));

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
            <FacetItemsList>{STATUSES.map(this.renderItem)}</FacetItemsList>
            <MultipleSelectionHint options={Object.keys(stats).length} values={statuses.length} />
          </>
        )}
      </FacetBox>
    );
  }
}
