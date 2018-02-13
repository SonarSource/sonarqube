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
// @flow
import React from 'react';
import { orderBy, without } from 'lodash';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import { translate } from '../../../helpers/l10n';
import { formatFacetStat } from '../utils';

/*::
type Props = {|
  facetMode: string,
  onChange: (changes: {}) => void,
  onToggle: (property: string) => void,
  open: boolean,
  resolved: boolean,
  resolutions: Array<string>,
  stats?: { [string]: number }
|};
*/

export default class ResolutionFacet extends React.PureComponent {
  /*:: props: Props; */

  property = 'resolutions';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue /*: string */) => {
    if (itemValue === '') {
      // unresolved
      this.props.onChange({ resolved: !this.props.resolved, resolutions: [] });
    } else {
      // defined resolution
      const { resolutions } = this.props;
      const newValue = orderBy(
        resolutions.includes(itemValue)
          ? without(resolutions, itemValue)
          : [...resolutions, itemValue]
      );
      this.props.onChange({ resolved: true, resolutions: newValue });
    }
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ resolved: false, resolutions: [] });
  };

  isFacetItemActive(resolution /*: string */) {
    return resolution === '' ? !this.props.resolved : this.props.resolutions.includes(resolution);
  }

  getFacetItemName(resolution /*: string */) {
    return resolution === '' ? translate('unresolved') : translate('issue.resolution', resolution);
  }

  getStat(resolution /*: string */) /*: ?number */ {
    const { stats } = this.props;
    return stats ? stats[resolution] : null;
  }

  renderItem = (resolution /*: string */) => {
    const active = this.isFacetItemActive(resolution);
    const stat = this.getStat(resolution);

    return (
      <FacetItem
        active={active}
        disabled={stat === 0 && !active}
        key={resolution}
        halfWidth={true}
        name={this.getFacetItemName(resolution)}
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat, this.props.facetMode)}
        value={resolution}
      />
    );
  };

  render() {
    const resolutions = ['', 'FIXED', 'FALSE-POSITIVE', 'WONTFIX', 'REMOVED'];
    const values = this.props.resolutions.map(resolution => this.getFacetItemName(resolution));

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && <FacetItemsList>{resolutions.map(this.renderItem)}</FacetItemsList>}
      </FacetBox>
    );
  }
}
