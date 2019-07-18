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
import { omit, sortBy, without } from 'lodash';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { highlightTerm } from 'sonar-ui-common/helpers/search';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import {
  getStandards,
  renderCWECategory,
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderSonarSourceSecurityCategory
} from '../../../helpers/security-standard';
import { Facet, formatFacetStat, Query, STANDARDS } from '../utils';

interface Props {
  cwe: string[];
  cweOpen: boolean;
  cweStats: T.Dict<number> | undefined;
  fetchingCwe: boolean;
  fetchingOwaspTop10: boolean;
  fetchingSansTop25: boolean;
  fetchingSonarSourceSecurity: boolean;
  loadSearchResultCount?: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  owaspTop10: string[];
  owaspTop10Open: boolean;
  owaspTop10Stats: T.Dict<number> | undefined;
  query: Partial<Query>;
  sansTop25: string[];
  sansTop25Open: boolean;
  sansTop25Stats: T.Dict<number> | undefined;
  sonarsourceSecurity: string[];
  sonarsourceSecurityOpen: boolean;
  sonarsourceSecurityStats: T.Dict<number> | undefined;
}

interface State {
  cweQuery: string;
  standards: T.Standards;
}

type StatsProp = 'owaspTop10Stats' | 'cweStats' | 'sansTop25Stats' | 'sonarsourceSecurityStats';
type ValuesProp = T.StandardType;

export default class StandardFacet extends React.PureComponent<Props, State> {
  mounted = false;
  property = STANDARDS;
  state: State = {
    cweQuery: '',
    standards: { owaspTop10: {}, sansTop25: {}, cwe: {}, sonarsourceSecurity: {} }
  };

  componentDidMount() {
    this.mounted = true;

    // load standards.json only if the facet is open, or there is a selected value
    if (
      this.props.open ||
      this.props.owaspTop10.length > 0 ||
      this.props.cwe.length > 0 ||
      this.props.sansTop25.length > 0 ||
      this.props.sonarsourceSecurity.length > 0
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
    getStandards().then(
      ({ owaspTop10, sansTop25, cwe, sonarsourceSecurity }: T.Standards) => {
        if (this.mounted) {
          this.setState({ standards: { owaspTop10, sansTop25, cwe, sonarsourceSecurity } });
        }
      },
      () => {}
    );
  };

  getValues = () => {
    return [
      ...this.props.sonarsourceSecurity.map(item =>
        renderSonarSourceSecurityCategory(this.state.standards, item, true)
      ),
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

  handleSonarSourceSecurityHeaderClick = () => {
    this.props.onToggle('sonarsourceSecurity');
  };

  handleClear = () => {
    this.props.onChange({
      [this.property]: [],
      owaspTop10: [],
      sansTop25: [],
      cwe: [],
      sonarsourceSecurity: []
    });
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

  handleSonarSourceSecurityItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('sonarsourceSecurity', itemValue, multiple);
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
    const { loadSearchResultCount } = this.props;
    return loadSearchResultCount
      ? loadSearchResultCount('cwe', { cwe: categories })
      : Promise.resolve({});
  };

  renderList = (
    statsProp: StatsProp,
    valuesProp: ValuesProp,
    renderName: (standards: T.Standards, category: string) => string,
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
    renderName: (standards: T.Standards, category: string) => React.ReactNode,
    renderTooltip: (standards: T.Standards, category: string) => string,
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

  renderSonarSourceSecurityList() {
    return this.renderList(
      'sonarsourceSecurityStats',
      'sonarsourceSecurity',
      renderSonarSourceSecurityCategory,
      this.handleSonarSourceSecurityItemClick
    );
  }

  renderSonarSourceSecurityHint() {
    return this.renderHint('sonarsourceSecurityStats', 'sonarsourceSecurity');
  }

  renderSubFacets() {
    return (
      <>
        <FacetBox className="is-inner" property="sonarsourceSecurity">
          <FacetHeader
            fetching={this.props.fetchingSonarSourceSecurity}
            name={translate('issues.facet.sonarsourceSecurity')}
            onClick={this.handleSonarSourceSecurityHeaderClick}
            open={this.props.sonarsourceSecurityOpen}
            values={this.props.sonarsourceSecurity.map(item =>
              renderSonarSourceSecurityCategory(this.state.standards, item)
            )}
          />
          {this.props.sonarsourceSecurityOpen && (
            <>
              {this.renderSonarSourceSecurityList()}
              {this.renderSonarSourceSecurityHint()}
            </>
          )}
        </FacetBox>
        <FacetBox className="is-inner" property="owaspTop10">
          <FacetHeader
            fetching={this.props.fetchingOwaspTop10}
            name={translate('issues.facet.owaspTop10')}
            onClick={this.handleOwaspTop10HeaderClick}
            open={this.props.owaspTop10Open}
            values={this.props.owaspTop10.map(item =>
              renderOwaspTop10Category(this.state.standards, item)
            )}
          />
          {this.props.owaspTop10Open && (
            <>
              {this.renderOwaspTop10List()}
              {this.renderOwaspTop10Hint()}
            </>
          )}
        </FacetBox>
        <FacetBox className="is-inner" property="sansTop25">
          <FacetHeader
            fetching={this.props.fetchingSansTop25}
            name={translate('issues.facet.sansTop25')}
            onClick={this.handleSansTop25HeaderClick}
            open={this.props.sansTop25Open}
            values={this.props.sansTop25.map(item =>
              renderSansTop25Category(this.state.standards, item)
            )}
          />
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
