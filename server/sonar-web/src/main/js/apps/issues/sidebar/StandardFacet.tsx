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
import { sortBy, without } from 'lodash';
import { Query, STANDARDS, formatFacetStat } from '../utils';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import { translate } from '../../../helpers/l10n';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import FacetItem from '../../../components/facet/FacetItem';
import Select from '../../../components/controls/Select';
import {
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderCWECategory,
  Standards
} from '../../securityReports/utils';

export interface Props {
  cwe: string[];
  cweOpen: boolean;
  cweStats: { [x: string]: number } | undefined;
  loading?: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  owaspTop10: string[];
  owaspTop10Open: boolean;
  owaspTop10Stats: { [x: string]: number } | undefined;
  sansTop25: string[];
  sansTop25Open: boolean;
  sansTop25Stats: { [x: string]: number } | undefined;
}

interface State {
  standards: Standards;
}

export default class StandardFacet extends React.PureComponent<Props, State> {
  mounted = false;
  property = STANDARDS;
  state: State = { standards: { owaspTop10: {}, sansTop25: {}, cwe: {} } };

  componentDidMount() {
    this.mounted = true;
    if (this.props.open) {
      this.loadStandards();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.open && this.props.open) {
      this.loadStandards();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStandards = () => {
    import('../../../helpers/standards.json')
      .then(x => x.default)
      .then(
        ({ owaspTop10, sansTop25, cwe }: Standards) => {
          if (this.mounted) {
            this.setState({ standards: { owaspTop10, sansTop25, cwe } });
          }
        },
        () => {}
      );
  };

  getValues = () => {
    return [
      ...this.props.owaspTop10.map(item => renderOwaspTop10Category(this.state.standards, item)),
      ...this.props.sansTop25.map(item => renderSansTop25Category(this.state.standards, item)),
      ...this.props.cwe.map(item => renderCWECategory(this.state.standards, item))
    ];
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleOwaspTop10HeaderClick = () => {
    this.props.onToggle('owaspTop10');
  };

  handleSansTop25HeaderClick = () => {
    this.props.onToggle('sansTop25');
  };

  handleCWEHeaderClick = () => {
    this.props.onToggle('cwe');
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [], owaspTop10: [], sansTop25: [], cwe: [] });
  };

  handleItemClick = (
    prop: 'owaspTop10' | 'sansTop25' | 'cwe',
    itemValue: string,
    multiple: boolean
  ) => {
    const items = this.props[prop];
    if (multiple) {
      const newValue = sortBy(
        items.includes(itemValue) ? without(items, itemValue) : [...items, itemValue]
      );
      this.props.onChange({ [prop]: newValue });
    } else {
      this.props.onChange({
        [prop]: items.includes(itemValue) && items.length < 2 ? [] : [itemValue]
      });
    }
  };

  handleOwaspTop10ItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('owaspTop10', itemValue, multiple);
  };

  handleCWEItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('cwe', itemValue, multiple);
  };

  handleSansTop25ItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('sansTop25', itemValue, multiple);
  };

  handleCWESelect = ({ value }: { value: string }) => {
    this.handleItemClick('cwe', value, true);
  };

  renderList = (
    statsProp: 'owaspTop10Stats' | 'cweStats' | 'sansTop25Stats',
    valuesProp: 'owaspTop10' | 'cwe' | 'sansTop25',
    renderName: (standards: Standards, category: string) => string,
    onClick: (x: string, multiple?: boolean) => void
  ) => {
    const stats = this.props[statsProp];
    const values = this.props[valuesProp];

    if (!stats) {
      return null;
    }

    const categories = sortBy(Object.keys(stats), key => -stats[key]);

    if (!categories.length) {
      return (
        <div className="search-navigator-facet-empty little-spacer-top">
          {translate('no_results')}
        </div>
      );
    }

    const getStat = (category: string) => {
      return stats ? stats[category] : undefined;
    };

    return (
      <FacetItemsList>
        {categories.map(category => (
          <FacetItem
            active={values.includes(category)}
            key={category}
            loading={this.props.loading}
            name={renderName(this.state.standards, category)}
            onClick={onClick}
            stat={formatFacetStat(getStat(category))}
            tooltip={values.length === 1 && !values.includes(category)}
            value={category}
          />
        ))}
      </FacetItemsList>
    );
  };

  renderOwaspTop10List() {
    return this.renderList(
      'owaspTop10Stats',
      'owaspTop10',
      renderOwaspTop10Category,
      this.handleOwaspTop10ItemClick
    );
  }

  renderCWEList() {
    return this.renderList('cweStats', 'cwe', renderCWECategory, this.handleCWEItemClick);
  }

  renderCWESearch() {
    const options = Object.keys(this.state.standards.cwe).map(cwe => ({
      label: renderCWECategory(this.state.standards, cwe),
      value: cwe
    }));
    return (
      <div className="search-navigator-facet-footer">
        <Select
          className="input-super-large"
          clearable={false}
          noResultsText={translate('select2.noMatches')}
          onChange={this.handleCWESelect}
          options={options}
          placeholder={translate('search.search_for_cwe')}
          searchable={true}
        />
      </div>
    );
  }

  renderSansTop25List() {
    return this.renderList(
      'sansTop25Stats',
      'sansTop25',
      renderSansTop25Category,
      this.handleSansTop25ItemClick
    );
  }

  renderSubFacets() {
    return (
      <>
        <FacetBox className="is-inner" property="owaspTop10">
          <FacetHeader
            name={translate('issues.facet.owaspTop10')}
            onClick={this.handleOwaspTop10HeaderClick}
            open={this.props.owaspTop10Open}
            values={this.props.owaspTop10.map(item =>
              renderOwaspTop10Category(this.state.standards, item)
            )}
          />
          {this.props.owaspTop10Open && this.renderOwaspTop10List()}
        </FacetBox>
        <FacetBox className="is-inner" property="sansTop25">
          <FacetHeader
            name={translate('issues.facet.sansTop25')}
            onClick={this.handleSansTop25HeaderClick}
            open={this.props.sansTop25Open}
            values={this.props.sansTop25.map(item =>
              renderSansTop25Category(this.state.standards, item)
            )}
          />
          {this.props.sansTop25Open && this.renderSansTop25List()}
        </FacetBox>
        <FacetBox className="is-inner" property="cwe">
          <FacetHeader
            name={translate('issues.facet.cwe')}
            onClick={this.handleCWEHeaderClick}
            open={this.props.cweOpen}
            values={this.props.cwe.map(item => renderCWECategory(this.state.standards, item))}
          />
          {this.props.cweOpen && this.renderCWEList()}
          {this.props.cweOpen && this.renderCWESearch()}
        </FacetBox>
      </>
    );
  }

  render() {
    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={this.getValues()}
        />

        {this.props.open && this.renderSubFacets()}
      </FacetBox>
    );
  }
}
