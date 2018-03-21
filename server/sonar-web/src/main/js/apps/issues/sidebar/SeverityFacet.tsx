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
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { translate } from '../../../helpers/l10n';

interface Props {
  facetMode: string;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  severities: string[];
  stats: { [x: string]: number } | undefined;
}

export default class SeverityFacet extends React.PureComponent<Props> {
  property = 'severities';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue: string) => {
    const { severities } = this.props;
    const newValue = orderBy(
      severities.includes(itemValue) ? without(severities, itemValue) : [...severities, itemValue]
    );
    this.props.onChange({ [this.property]: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [] });
  };

  getStat(severity: string) {
    const { stats } = this.props;
    return stats ? stats[severity] : undefined;
  }

  renderItem = (severity: string) => {
    const active = this.props.severities.includes(severity);
    const stat = this.getStat(severity);

    return (
      <FacetItem
        active={active}
        disabled={stat === 0 && !active}
        halfWidth={true}
        key={severity}
        name={<SeverityHelper severity={severity} />}
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat, this.props.facetMode)}
        value={severity}
      />
    );
  };

  render() {
    const severities = ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'];
    const values = this.props.severities.map(severity => translate('severity', severity));

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && <FacetItemsList>{severities.map(this.renderItem)}</FacetItemsList>}
      </FacetBox>
    );
  }
}
