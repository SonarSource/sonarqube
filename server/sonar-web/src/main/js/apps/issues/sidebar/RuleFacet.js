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
import { searchRules } from '../../../api/rules';
import { translate } from '../../../helpers/l10n';
import { formatFacetStat } from '../utils';

/*::
type Props = {|
  facetMode: string,
  languages: Array<string>,
  onChange: (changes: { [string]: Array<string> }) => void,
  onToggle: (property: string) => void,
  organization: string | void;
  open: boolean,
  stats?: { [string]: number },
  referencedRules: { [string]: { name: string } },
  rules: Array<string>
|};
*/

export default class RuleFacet extends React.PureComponent {
  /*:: props: Props; */

  property = 'rules';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue /*: string */) => {
    const { rules } = this.props;
    const newValue = sortBy(
      rules.includes(itemValue) ? without(rules, itemValue) : [...rules, itemValue]
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
    const { languages, organization } = this.props;
    return searchRules({
      f: 'name,langName',
      languages: languages.length ? languages.join() : undefined,
      organization,
      q: query
    }).then(response =>
      response.rules.map(rule => ({ label: `(${rule.langName}) ${rule.name}`, value: rule.key }))
    );
  };

  handleSelect = (rule /*: string */) => {
    const { rules } = this.props;
    this.props.onChange({ [this.property]: uniq([...rules, rule]) });
  };

  getRuleName(rule /*: string */) /*: string */ {
    const { referencedRules } = this.props;
    return referencedRules[rule] ? referencedRules[rule].name : rule;
  }

  getStat(rule /*: string */) /*: ?number */ {
    const { stats } = this.props;
    return stats ? stats[rule] : null;
  }

  renderList() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const rules = sortBy(Object.keys(stats), key => -stats[key]);

    return (
      <FacetItemsList>
        {rules.map(rule => (
          <FacetItem
            active={this.props.rules.includes(rule)}
            key={rule}
            name={this.getRuleName(rule)}
            onClick={this.handleItemClick}
            stat={formatFacetStat(this.getStat(rule), this.props.facetMode)}
            value={rule}
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
    const values = this.props.rules.map(rule => this.getRuleName(rule));
    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && this.renderList()}
        {this.props.open && this.renderFooter()}
      </FacetBox>
    );
  }
}
