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

/* eslint-disable react/no-unused-prop-types */

import { omit, sortBy, without } from 'lodash';
import * as React from 'react';
import { FacetBox, FacetItem, Note, TextMuted } from '~design-system';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import {
  getStandards,
  renderCWECategory,
  renderOwaspTop102021Category,
  renderOwaspTop10Category,
  renderSonarSourceSecurityCategory,
} from '../../../helpers/security-standard';
import { Facet } from '../../../types/issues';
import { SecurityStandard, Standards } from '../../../types/security';
import { Dict } from '../../../types/types';
import { Query, STANDARDS, formatFacetStat } from '../utils';
import { FacetItemsList } from './FacetItemsList';
import { ListStyleFacet } from './ListStyleFacet';
import { ListStyleFacetFooter } from './ListStyleFacetFooter';
import { MultipleSelectionHint } from './MultipleSelectionHint';

interface Props {
  cwe: string[];
  cweOpen: boolean;
  cweStats: Dict<number> | undefined;
  fetchingCwe: boolean;
  fetchingOwaspTop10: boolean;
  'fetchingOwaspTop10-2021': boolean;
  fetchingSonarSourceSecurity: boolean;
  loadSearchResultCount?: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  owaspTop10: string[];
  'owaspTop10-2021': string[];
  'owaspTop10-2021Open': boolean;
  'owaspTop10-2021Stats': Dict<number> | undefined;
  owaspTop10Open: boolean;
  owaspTop10Stats: Dict<number> | undefined;
  query: Partial<Query>;
  sonarsourceSecurity: string[];
  sonarsourceSecurityOpen: boolean;
  sonarsourceSecurityStats: Dict<number> | undefined;
}

interface State {
  showFullSonarSourceList: boolean;
  standards: Standards;
}

type StatsProp =
  | 'owaspTop10-2021Stats'
  | 'owaspTop10Stats'
  | 'cweStats'
  | 'sonarsourceSecurityStats';

type ValuesProp = 'owaspTop10-2021' | 'owaspTop10' | 'sonarsourceSecurity' | 'cwe';

const INITIAL_FACET_COUNT = 15;

export class StandardFacet extends React.PureComponent<Props, State> {
  mounted = false;
  property = STANDARDS;

  state: State = {
    showFullSonarSourceList: false,
    standards: {
      owaspTop10: {},
      'owaspTop10-2021': {},
      cwe: {},
      sonarsourceSecurity: {},
      'pciDss-3.2': {},
      'pciDss-4.0': {},
      'stig-ASD_V5R3': {},
      casa: {},
      'owaspAsvs-4.0': {},
    },
  };

  componentDidMount() {
    this.mounted = true;

    // load standards.json only if the facet is open, or there is a selected value
    if (
      this.props.open ||
      this.props.owaspTop10.length > 0 ||
      this.props['owaspTop10-2021'].length > 0 ||
      this.props.cwe.length > 0 ||
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
      ({
        [SecurityStandard.OWASP_TOP10_2021]: owaspTop102021,
        [SecurityStandard.OWASP_TOP10]: owaspTop10,
        [SecurityStandard.CWE]: cwe,
        [SecurityStandard.SONARSOURCE]: sonarsourceSecurity,
        [SecurityStandard.PCI_DSS_3_2]: pciDss32,
        [SecurityStandard.PCI_DSS_4_0]: pciDss40,
        [SecurityStandard.OWASP_ASVS_4_0]: owaspAsvs40,
        [SecurityStandard.STIG_ASD_V5R3]: stigV5,
        [SecurityStandard.CASA]: casa,
      }: Standards) => {
        if (this.mounted) {
          this.setState({
            standards: {
              [SecurityStandard.OWASP_TOP10_2021]: owaspTop102021,
              [SecurityStandard.OWASP_TOP10]: owaspTop10,
              [SecurityStandard.CWE]: cwe,
              [SecurityStandard.SONARSOURCE]: sonarsourceSecurity,
              [SecurityStandard.PCI_DSS_3_2]: pciDss32,
              [SecurityStandard.PCI_DSS_4_0]: pciDss40,
              [SecurityStandard.OWASP_ASVS_4_0]: owaspAsvs40,
              [SecurityStandard.STIG_ASD_V5R3]: stigV5,
              [SecurityStandard.CASA]: casa,
            },
          });
        }
      },
      () => {},
    );
  };

  getValues = () => {
    return [
      ...this.props.sonarsourceSecurity.map((item) =>
        renderSonarSourceSecurityCategory(this.state.standards, item, true),
      ),
      ...this.props.owaspTop10.map((item) =>
        renderOwaspTop10Category(this.state.standards, item, true),
      ),
      ...this.props['owaspTop10-2021'].map((item) =>
        renderOwaspTop102021Category(this.state.standards, item, true),
      ),
      ...this.props.cwe.map((item) => renderCWECategory(this.state.standards, item)),
    ];
  };

  getFacetHeaderId = (property: string) => {
    return `facet_${property}`;
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleOwaspTop10HeaderClick = () => {
    this.props.onToggle('owaspTop10');
  };

  handleOwaspTop102021HeaderClick = () => {
    this.props.onToggle('owaspTop10-2021');
  };

  handleSonarSourceSecurityHeaderClick = () => {
    this.props.onToggle('sonarsourceSecurity');
  };

  handleClear = () => {
    this.props.onChange({
      [this.property]: [],
      owaspTop10: [],
      'owaspTop10-2021': [],
      cwe: [],
      sonarsourceSecurity: [],
    });
  };

  handleItemClick = (prop: ValuesProp, itemValue: string, multiple: boolean) => {
    const items = this.props[prop];

    if (multiple) {
      const newValue = sortBy(
        items.includes(itemValue) ? without(items, itemValue) : [...items, itemValue],
      );

      this.props.onChange({ [prop]: newValue });
    } else {
      this.props.onChange({
        [prop]: items.includes(itemValue) && items.length < 2 ? [] : [itemValue],
      });
    }
  };

  handleOwaspTop10ItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick(SecurityStandard.OWASP_TOP10, itemValue, multiple);
  };

  handleOwaspTop102021ItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick(SecurityStandard.OWASP_TOP10_2021, itemValue, multiple);
  };

  handleSonarSourceSecurityItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick(SecurityStandard.SONARSOURCE, itemValue, multiple);
  };

  handleCWESearch = (query: string) => {
    return Promise.resolve({
      results: Object.keys(this.state.standards.cwe).filter((cwe) =>
        renderCWECategory(this.state.standards, cwe).toLowerCase().includes(query.toLowerCase()),
      ),
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
    renderName: (standards: Standards, category: string) => string,
    onClick: (x: string, multiple?: boolean) => void,
  ) => {
    const stats = this.props[statsProp];
    const values = this.props[valuesProp];

    if (!stats) {
      return null;
    }

    const categories = sortBy(Object.keys(stats), (key) => -stats[key]);

    return this.renderFacetItemsList(stats, values, categories, renderName, renderName, onClick);
  };

  // eslint-disable-next-line max-params
  renderFacetItemsList = (
    stats: Dict<number | undefined>,
    values: string[],
    categories: string[],
    renderName: (standards: Standards, category: string) => React.ReactNode,
    renderTooltip: (standards: Standards, category: string) => string,
    onClick: (x: string, multiple?: boolean) => void,
  ) => {
    if (!categories.length) {
      return <TextMuted className="sw-ml-2 sw-mt-1" text={translate('no_results')} />;
    }

    const getStat = (category: string) => {
      return stats ? stats[category] : undefined;
    };

    return categories.map((category) => (
      <FacetItem
        active={values.includes(category)}
        className="it__search-navigator-facet"
        key={category}
        name={renderName(this.state.standards, category)}
        onClick={onClick}
        stat={formatFacetStat(getStat(category)) ?? 0}
        tooltip={renderTooltip(this.state.standards, category)}
        value={category}
      />
    ));
  };

  renderHint = (statsProp: StatsProp, valuesProp: ValuesProp) => {
    const nbSelectableItems = Object.keys(this.props[statsProp] ?? {}).length;
    const nbSelectedItems = this.props[valuesProp].length;

    return (
      <MultipleSelectionHint
        nbSelectableItems={nbSelectableItems}
        nbSelectedItems={nbSelectedItems}
      />
    );
  };

  renderOwaspTop10List() {
    return this.renderList(
      'owaspTop10Stats',
      SecurityStandard.OWASP_TOP10,
      renderOwaspTop10Category,
      this.handleOwaspTop10ItemClick,
    );
  }

  renderOwaspTop102021List() {
    return this.renderList(
      'owaspTop10-2021Stats',
      SecurityStandard.OWASP_TOP10_2021,
      renderOwaspTop102021Category,
      this.handleOwaspTop102021ItemClick,
    );
  }

  renderSonarSourceSecurityList() {
    const stats = this.props.sonarsourceSecurityStats;
    const values = this.props.sonarsourceSecurity;

    if (!stats) {
      return null;
    }

    const sortedItems = sortBy(
      Object.keys(stats),
      (key) => -stats[key],
      (key) => renderSonarSourceSecurityCategory(this.state.standards, key),
    );

    const limitedList = this.state.showFullSonarSourceList
      ? sortedItems
      : sortedItems.slice(0, INITIAL_FACET_COUNT);

    // make sure all selected items are displayed
    const selectedBelowLimit = this.state.showFullSonarSourceList
      ? []
      : sortedItems.slice(INITIAL_FACET_COUNT).filter((item) => values.includes(item));

    const allItemShown = limitedList.length + selectedBelowLimit.length === sortedItems.length;

    if (!(limitedList.length || selectedBelowLimit.length)) {
      return <TextMuted className="sw-ml-2 sw-mt-1" text={translate('no_results')} />;
    }

    return (
      <>
        {limitedList.map((item) => (
          <FacetItem
            active={values.includes(item)}
            className="it__search-navigator-facet"
            key={item}
            name={renderSonarSourceSecurityCategory(this.state.standards, item)}
            onClick={this.handleSonarSourceSecurityItemClick}
            stat={formatFacetStat(stats[item]) ?? 0}
            tooltip={renderSonarSourceSecurityCategory(this.state.standards, item)}
            value={item}
          />
        ))}

        {selectedBelowLimit.length > 0 && (
          <>
            {!allItemShown && (
              <Note as="div" className="sw-mb-2 sw-text-center">
                ⋯
              </Note>
            )}
            {selectedBelowLimit.map((item) => (
              <FacetItem
                active
                className="it__search-navigator-facet"
                key={item}
                name={renderSonarSourceSecurityCategory(this.state.standards, item)}
                onClick={this.handleSonarSourceSecurityItemClick}
                stat={formatFacetStat(stats[item]) ?? 0}
                tooltip={renderSonarSourceSecurityCategory(this.state.standards, item)}
                value={item}
              />
            ))}
          </>
        )}

        {!allItemShown && (
          <ListStyleFacetFooter
            nbShown={limitedList.length + selectedBelowLimit.length}
            showMore={() => this.setState({ showFullSonarSourceList: true })}
            showMoreAriaLabel={translate('issues.facet.sonarsource.show_more')}
            total={sortedItems.length}
          />
        )}
      </>
    );
  }

  renderOwaspTop10Hint() {
    return this.renderHint('owaspTop10Stats', SecurityStandard.OWASP_TOP10);
  }

  renderOwaspTop102021Hint() {
    return this.renderHint('owaspTop10-2021Stats', SecurityStandard.OWASP_TOP10_2021);
  }

  renderSonarSourceSecurityHint() {
    return this.renderHint('sonarsourceSecurityStats', SecurityStandard.SONARSOURCE);
  }

  renderSubFacets() {
    const {
      cwe,
      cweOpen,
      cweStats,
      fetchingCwe,
      fetchingOwaspTop10,
      'fetchingOwaspTop10-2021': fetchingOwaspTop102021,
      fetchingSonarSourceSecurity,
      owaspTop10,
      owaspTop10Open,
      'owaspTop10-2021Open': owaspTop102021Open,
      'owaspTop10-2021': owaspTop102021,
      query,
      sonarsourceSecurity,
      sonarsourceSecurityOpen,
    } = this.props;

    const standards = [
      {
        count: sonarsourceSecurity.length,
        loading: fetchingSonarSourceSecurity,
        name: 'sonarsourceSecurity',
        onClick: this.handleSonarSourceSecurityHeaderClick,
        open: sonarsourceSecurityOpen,
        panel: (
          <>
            {this.renderSonarSourceSecurityList()}
            {this.renderSonarSourceSecurityHint()}
          </>
        ),
        property: SecurityStandard.SONARSOURCE,
      },
      {
        count: owaspTop102021.length,
        loading: fetchingOwaspTop102021,
        name: 'owaspTop10_2021',
        onClick: this.handleOwaspTop102021HeaderClick,
        open: owaspTop102021Open,
        panel: (
          <>
            {this.renderOwaspTop102021List()}
            {this.renderOwaspTop102021Hint()}
          </>
        ),
        property: SecurityStandard.OWASP_TOP10_2021,
      },
      {
        count: owaspTop10.length,
        loading: fetchingOwaspTop10,
        name: 'owaspTop10',
        onClick: this.handleOwaspTop10HeaderClick,
        open: owaspTop10Open,
        panel: (
          <>
            {this.renderOwaspTop10List()}
            {this.renderOwaspTop10Hint()}
          </>
        ),
        property: SecurityStandard.OWASP_TOP10,
      },
    ];

    return (
      <>
        {standards.map(({ name, open, panel, property, ...standard }) => (
          <FacetBox
            className="it__search-navigator-facet-box it__search-navigator-facet-header"
            data-property={property}
            id={this.getFacetHeaderId(property)}
            inner
            key={property}
            name={translate(`issues.facet.${name}`)}
            open={open}
            {...standard}
          >
            <FacetItemsList labelledby={this.getFacetHeaderId(property)}>{panel}</FacetItemsList>
          </FacetBox>
        ))}

        <ListStyleFacet<string>
          facetHeader={translate('issues.facet.cwe')}
          fetching={fetchingCwe}
          getFacetItemText={(item) => renderCWECategory(this.state.standards, item)}
          getSearchResultText={(item) => renderCWECategory(this.state.standards, item)}
          inner
          loadSearchResultCount={this.loadCWESearchResultCount}
          onChange={this.props.onChange}
          onSearch={this.handleCWESearch}
          onToggle={this.props.onToggle}
          open={cweOpen}
          property={SecurityStandard.CWE}
          query={omit(query, 'cwe')}
          renderFacetItem={(item) => renderCWECategory(this.state.standards, item)}
          renderSearchResult={(item, query) =>
            highlightTerm(renderCWECategory(this.state.standards, item), query)
          }
          searchPlaceholder={translate('search.search_for_cwe')}
          searchInputAriaLabel={translate('search.search_for_cwe')}
          stats={cweStats}
          values={cwe}
        />
      </>
    );
  }

  render() {
    const { open } = this.props;
    const count = this.getValues().length;

    return (
      <FacetBox
        className="it__search-navigator-facet-box it__search-navigator-facet-header"
        count={count}
        countLabel={translateWithParameters('x_selected', count)}
        data-property={this.property}
        hasEmbeddedFacets
        id={this.getFacetHeaderId(this.property)}
        name={translate('issues.facet', this.property)}
        onClear={this.handleClear}
        onClick={this.handleHeaderClick}
        open={open}
      >
        {this.renderSubFacets()}
      </FacetBox>
    );
  }
}
