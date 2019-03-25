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
import * as React from 'react';
import { sortBy, without, omit } from 'lodash';
import { Query, STANDARDS, formatFacetStat, Facet } from '../utils';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import { translate } from '../../../helpers/l10n';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import FacetItem from '../../../components/facet/FacetItem';
import {
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderCWECategory,
  Standards
} from '../../securityReports/utils';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import { highlightTerm } from '../../../helpers/search';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';

export interface Props {
  cwe: string[];
  cweOpen: boolean;
  cweStats: T.Dict<number> | undefined;
  fetchingOwaspTop10: boolean;
  fetchingSansTop25: boolean;
  fetchingCwe: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  owaspTop10: string[];
  owaspTop10Open: boolean;
  owaspTop10Stats: T.Dict<number> | undefined;
  query: Query;
  sansTop25: string[];
  sansTop25Open: boolean;
  sansTop25Stats: T.Dict<number> | undefined;
}

interface State {
  cweQuery: string;
  standards: Standards;
}

type StatsProp = 'owaspTop10Stats' | 'cweStats' | 'sansTop25Stats';
type ValuesProp = 'owaspTop10' | 'sansTop25' | 'cwe';

export default class StandardFacet extends React.PureComponent<Props, State> {
  mounted = false;
  property = STANDARDS;
  state: State = {
    cweQuery: '',
    standards: { owaspTop10: {}, sansTop25: {}, cwe: {} }
  };

  componentDidMount() {
    this.mounted = true;

    // load standards.json only if the facet is open, or there is a selected value
    if (
      this.props.open ||
      this.props.owaspTop10.length > 0 ||
      this.props.cwe.length > 0 ||
      this.props.sansTop25.length > 0
    ) {
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
      ...this.props.owaspTop10.map(item =>
        renderOwaspTop10Category(this.state.standards, item, true)
      ),
      ...this.props.sansTop25.map(item =>
        renderSansTop25Category(this.state.standards, item, true)
      ),
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

  handleClear = () => {
    this.props.onChange({ [this.property]: [], owaspTop10: [], sansTop25: [], cwe: [] });
  };

  handleItemClick = (prop: ValuesProp, itemValue: string, multiple: boolean) => {
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

  handleSansTop25ItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('sansTop25', itemValue, multiple);
  };

  handleCWESearch = (query: string) => {
    return Promise.resolve({
      results: Object.keys(this.state.standards.cwe).filter(cwe =>
        renderCWECategory(this.state.standards, cwe)
          .toLowerCase()
          .includes(query.toLowerCase())
      )
    });
  };

  loadCWESearchResultCount = (categories: string[]) => {
    return this.props.loadSearchResultCount('cwe', { cwe: categories });
  };

  renderList = (
    statsProp: StatsProp,
    valuesProp: ValuesProp,
    renderName: (standards: Standards, category: string) => string,
    onClick: (x: string, multiple?: boolean) => void
  ) => {
    const stats = this.props[statsProp];
    const values = this.props[valuesProp];
    if (!stats) {
      return null;
    }
    const categories = sortBy(Object.keys(stats), key => -stats[key]);
    return this.renderFacetItemsList(stats, values, categories, renderName, renderName, onClick);
  };

  // eslint-disable-next-line max-params
  renderFacetItemsList = (
    stats: any,
    values: string[],
    categories: string[],
    renderName: (standards: Standards, category: string) => React.ReactNode,
    renderTooltip: (standards: Standards, category: string) => string,
    onClick: (x: string, multiple?: boolean) => void
  ) => {
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
            name={renderName(this.state.standards, category)}
            onClick={onClick}
            stat={formatFacetStat(getStat(category))}
            tooltip={renderTooltip(this.state.standards, category)}
            value={category}
          />
        ))}
      </FacetItemsList>
    );
  };

  renderHint = (statsProp: StatsProp, valuesProp: ValuesProp) => {
    const stats = this.props[statsProp] || {};
    const values = this.props[valuesProp];
    return <MultipleSelectionHint options={Object.keys(stats).length} values={values.length} />;
  };

  renderOwaspTop10List() {
    return this.renderList(
      'owaspTop10Stats',
      'owaspTop10',
      renderOwaspTop10Category,
      this.handleOwaspTop10ItemClick
    );
  }

  renderOwaspTop10Hint() {
    return this.renderHint('owaspTop10Stats', 'owaspTop10');
  }

  renderSansTop25List() {
    return this.renderList(
      'sansTop25Stats',
      'sansTop25',
      renderSansTop25Category,
      this.handleSansTop25ItemClick
    );
  }

  renderSansTop25Hint() {
    return this.renderHint('sansTop25Stats', 'sansTop25');
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
          <DeferredSpinner loading={this.props.fetchingOwaspTop10} />
          {this.props.owaspTop10Open && (
            <>
              {this.renderOwaspTop10List()}
              {this.renderOwaspTop10Hint()}
            </>
          )}
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
          <DeferredSpinner loading={this.props.fetchingSansTop25} />
          {this.props.sansTop25Open && (
            <>
              {this.renderSansTop25List()}
              {this.renderSansTop25Hint()}
            </>
          )}
        </FacetBox>
        <ListStyleFacet<string>
          className="is-inner"
          facetHeader={translate('issues.facet.cwe')}
          fetching={this.props.fetchingCwe}
          getFacetItemText={item => renderCWECategory(this.state.standards, item)}
          getSearchResultKey={item => item}
          getSearchResultText={item => renderCWECategory(this.state.standards, item)}
          loadSearchResultCount={this.loadCWESearchResultCount}
          onChange={this.props.onChange}
          onSearch={this.handleCWESearch}
          onToggle={this.props.onToggle}
          open={this.props.cweOpen}
          property="cwe"
          query={omit(this.props.query, 'cwe')}
          renderFacetItem={item => renderCWECategory(this.state.standards, item)}
          renderSearchResult={(item, query) =>
            highlightTerm(renderCWECategory(this.state.standards, item), query)
          }
          searchPlaceholder={translate('search.search_for_cwe')}
          stats={this.props.cweStats}
          values={this.props.cwe}
        />
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
