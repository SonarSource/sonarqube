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
import { sortBy, uniq, without } from 'lodash';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import FacetFooter from '../../../components/facet/FacetFooter';
import { searchIssueTags } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';
/*:: import type { Component } from '../utils'; */
import { formatFacetStat } from '../utils';

/*::
type Props = {|
  component?: Component,
  facetMode: string,
  onChange: (changes: { [string]: Array<string> }) => void,
  onToggle: (property: string) => void,
  open: boolean,
  organization?: { key: string },
  stats?: { [string]: number },
  tags: Array<string>
|};
*/

export default class TagFacet extends React.PureComponent {
  /*:: props: Props; */

  property = 'tags';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue /*: string */) => {
    const { tags } = this.props;
    const newValue = sortBy(
      tags.includes(itemValue) ? without(tags, itemValue) : [...tags, itemValue]
    );
    this.props.onChange({ [this.property]: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [] });
  };

  handleSearch = (query /*: string */) => {
    let organization = this.props.component && this.props.component.organization;
    if (this.props.organization && !organization) {
      organization = this.props.organization.key;
    }
    return searchIssueTags({ organization, ps: 50, q: query }).then(tags =>
      tags.map(tag => ({ label: tag, value: tag }))
    );
  };

  handleSelect = (tag /*: string */) => {
    const { tags } = this.props;
    this.props.onChange({ [this.property]: uniq([...tags, tag]) });
  };

  getStat(tag /*: string */) /*: ?number */ {
    const { stats } = this.props;
    return stats ? stats[tag] : null;
  }

  renderTag(tag /*: string */) {
    return (
      <span>
        <i className="icon-tags icon-gray little-spacer-right" />
        {tag}
      </span>
    );
  }

  renderList() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const tags = sortBy(Object.keys(stats), key => -stats[key]);

    return (
      <FacetItemsList>
        {tags.map(tag => (
          <FacetItem
            active={this.props.tags.includes(tag)}
            key={tag}
            name={this.renderTag(tag)}
            onClick={this.handleItemClick}
            stat={formatFacetStat(this.getStat(tag), this.props.facetMode)}
            value={tag}
          />
        ))}
      </FacetItemsList>
    );
  }

  renderFooter() {
    if (!this.props.stats) {
      return null;
    }

    return <FacetFooter onSearch={this.handleSearch} onSelect={this.handleSelect} />;
  }

  render() {
    return (
      <FacetBox>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={this.props.tags}
        />

        {this.props.open && this.renderList()}

        {this.props.open && this.renderFooter()}
      </FacetBox>
    );
  }
}
