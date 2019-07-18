/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import { formatFacetStat, Query } from '../utils';

interface Props {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  resolved: boolean;
  resolutions: string[];
  stats: T.Dict<number> | undefined;
}

const RESOLUTIONS = ['', 'FALSE-POSITIVE', 'FIXED', 'REMOVED', 'WONTFIX'];

export default class ResolutionFacet extends React.PureComponent<Props> {
  property = 'resolutions';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue: string, multiple: boolean) => {
    const { resolutions } = this.props;
    if (itemValue === '') {
      // unresolved
      this.props.onChange({ resolved: !this.props.resolved, resolutions: [] });
    } else if (multiple) {
      const newValue = orderBy(
        resolutions.includes(itemValue)
          ? without(resolutions, itemValue)
          : [...resolutions, itemValue]
      );
      this.props.onChange({ resolved: true, [this.property]: newValue });
    } else {
      this.props.onChange({
        resolved: true,
        [this.property]:
          resolutions.includes(itemValue) && resolutions.length < 2 ? [] : [itemValue]
      });
    }
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ resolved: false, resolutions: [] });
  };

  isFacetItemActive(resolution: string) {
    return resolution === '' ? !this.props.resolved : this.props.resolutions.includes(resolution);
  }

  getFacetItemName(resolution: string) {
    return resolution === '' ? translate('unresolved') : translate('issue.resolution', resolution);
  }

  getStat(resolution: string) {
    const { stats } = this.props;
    return stats ? stats[resolution] : undefined;
  }

  renderItem = (resolution: string) => {
    const active = this.isFacetItemActive(resolution);
    const stat = this.getStat(resolution);

    return (
      <FacetItem
        active={active}
        disabled={stat === 0 && !active}
        halfWidth={true}
        key={resolution}
        name={this.getFacetItemName(resolution)}
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat)}
        tooltip={this.getFacetItemName(resolution)}
        value={resolution}
      />
    );
  };

  render() {
    const { resolutions, stats = {} } = this.props;
    const values = resolutions.map(resolution => this.getFacetItemName(resolution));

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          clearLabel="reset_verb"
          fetching={this.props.fetching}
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && (
          <>
            <FacetItemsList>{RESOLUTIONS.map(this.renderItem)}</FacetItemsList>
            <MultipleSelectionHint
              options={Object.keys(stats).length}
              values={resolutions.length}
            />
          </>
        )}
      </FacetBox>
    );
  }
}
