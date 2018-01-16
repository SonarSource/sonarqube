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
import * as classNames from 'classnames';
import { Query, FacetKey } from '../query';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

interface Props {
  disabled: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (facet: FacetKey) => void;
  open: boolean;
  stats?: { [x: string]: number };
  values: string[];
}

export default class ActivationSeverityFacet extends React.PureComponent<Props> {
  handleItemClick = (itemValue: string) => {
    const { values } = this.props;
    const newValue = orderBy(
      values.includes(itemValue) ? without(values, itemValue) : [...values, itemValue]
    );
    this.props.onChange({ activationSeverities: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle('activationSeverities');
  };

  handleClear = () => {
    this.props.onChange({ activationSeverities: [] });
  };

  getStat = (value: string) => {
    const { stats } = this.props;
    return stats && stats[value];
  };

  renderItem = (severity: string) => {
    const active = this.props.values.includes(severity);
    const stat = this.getStat(severity);

    return (
      <FacetItem
        active={active}
        disabled={stat === 0 && !active}
        halfWidth={true}
        key={severity}
        name={<SeverityHelper severity={severity} />}
        onClick={this.handleItemClick}
        stat={stat && formatMeasure(stat, 'SHORT_INT')}
        value={severity}
      />
    );
  };

  render() {
    const values = this.props.values.map(severity => translate('severity', severity));
    const items = ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'];

    return (
      <FacetBox
        className={classNames({ 'search-navigator-facet-box-forbidden': this.props.disabled })}>
        <FacetHeader
          helper={
            this.props.disabled
              ? translate('coding_rules.filters.active_severity.inactive')
              : undefined
          }
          name={translate('coding_rules.facet.active_severities')}
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
