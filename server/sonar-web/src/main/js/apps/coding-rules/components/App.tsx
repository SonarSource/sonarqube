/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import key from 'keymaster';
import { keyBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { connect } from 'react-redux';
import { withRouter, WithRouterProps } from 'react-router';
import { Profile, searchQualityProfiles } from '../../../api/quality-profiles';
import { getRulesApp, searchRules } from '../../../api/rules';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import FiltersHeader from '../../../components/common/FiltersHeader';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import ListFooter from '../../../components/controls/ListFooter';
import SearchBox from '../../../components/controls/SearchBox';
import BackIcon from '../../../components/icons/BackIcon';
import '../../../components/search-navigator.css';
import { translate } from '../../../helpers/l10n';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass
} from '../../../helpers/pages';
import { scrollToElement } from '../../../helpers/scrolling';
import { isLoggedIn } from '../../../helpers/users';
import { getCurrentUser, Store } from '../../../store/rootReducer';
import { SecurityStandard } from '../../../types/security';
import { CurrentUser, Dict, Paging, RawQuery, Rule, RuleActivation } from '../../../types/types';
import {
  shouldOpenSonarSourceSecurityFacet,
  shouldOpenStandardsChildFacet,
  shouldOpenStandardsFacet,
  STANDARDS
} from '../../issues/utils';
import {
  Activation,
  Actives,
  areQueriesEqual,
  FacetKey,
  Facets,
  getAppFacet,
  getOpen,
  getServerFacet,
  hasRuleKey,
  OpenFacets,
  parseQuery,
  Query,
  serializeQuery,
  shouldRequestFacet
} from '../query';
import '../styles.css';
import BulkChange from './BulkChange';
import FacetsList from './FacetsList';
import PageActions from './PageActions';
import RuleDetails from './RuleDetails';
import RuleListItem from './RuleListItem';

const PAGE_SIZE = 100;
const MAX_SEARCH_LENGTH = 200;
const LIMIT_BEFORE_LOAD_MORE = 5;

interface Props extends WithRouterProps {
  currentUser: CurrentUser;
}

interface State {
  actives?: Actives;
  canWrite?: boolean;
  facets?: Facets;
  loading: boolean;
  openFacets: OpenFacets;
  openRule?: Rule;
  paging?: Paging;
  query: Query;
  referencedProfiles: Dict<Profile>;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  rules: Rule[];
  selected?: string;
  usingPermalink?: boolean;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    const query = parseQuery(props.location.query);
    this.state = {
      loading: true,
      openFacets: {
        languages: true,
        owaspTop10: shouldOpenStandardsChildFacet({}, query, SecurityStandard.OWASP_TOP10),
        sansTop25: shouldOpenStandardsChildFacet({}, query, SecurityStandard.SANS_TOP25),
        sonarsourceSecurity: shouldOpenSonarSourceSecurityFacet({}, query),
        standards: shouldOpenStandardsFacet({}, query),
        types: true
      },
      query,
      referencedProfiles: {},
      referencedRepositories: {},
      rules: []
    };
  }

  componentDidMount() {
    this.mounted = true;
    addWhitePageClass();
    addSideBarClass();
    this.attachShortcuts();
    this.fetchInitialData();
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState(({ rules, selected }) => {
      const openRule = this.getOpenRule(nextProps, rules);
      return {
        openRule,
        usingPermalink: hasRuleKey(nextProps.location.query),
        query: parseQuery(nextProps.location.query),
        selected: openRule ? openRule.key : selected
      };
    });
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    if (!areQueriesEqual(prevProps.location.query, this.props.location.query)) {
      this.fetchFirstRules();
    }
    if (
      !this.state.openRule &&
      (prevState.selected !== this.state.selected || prevState.openRule)
    ) {
      // if user simply selected another issue
      // or if user went from the source code back to the list of issues
      this.scrollToSelectedRule();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
    removeSideBarClass();
    this.detachShortcuts();
  }

  attachShortcuts = () => {
    key.setScope('coding-rules');
    key('up', 'coding-rules', () => {
      this.selectPreviousRule();
      return false;
    });
    key('down', 'coding-rules', () => {
      this.selectNextRule();
      return false;
    });
    key('right', 'coding-rules', () => {
      this.openSelectedRule();
      return false;
    });
    key('left', 'coding-rules', () => {
      this.handleBack();
      return false;
    });
  };

  detachShortcuts = () => key.deleteScope('coding-rules');

  getOpenRule = (props: Props, rules: Rule[]) => {
    const open = getOpen(props.location.query);
    return open && rules.find(rule => rule.key === open);
  };

  getFacetsToFetch = () => {
    const { openFacets } = this.state;
    return Object.keys(openFacets)
      .filter((facet: FacetKey) => openFacets[facet])
      .filter((facet: FacetKey) => shouldRequestFacet(facet))
      .map((facet: FacetKey) => getServerFacet(facet));
  };

  getFieldsToFetch = () => {
    const fields = [
      'isTemplate',
      'name',
      'lang',
      'langName',
      'severity',
      'status',
      'sysTags',
      'tags',
      'templateKey'
    ];
    if (this.state.query.profile) {
      fields.push('actives', 'params');
    }
    return fields;
  };

  getSearchParameters = () => ({
    f: this.getFieldsToFetch().join(),
    facets: this.getFacetsToFetch().join(),
    ps: PAGE_SIZE,
    s: 'name',
    ...serializeQuery(this.state.query)
  });

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchInitialData = () => {
    this.setState({ loading: true });
    Promise.all([getRulesApp(), searchQualityProfiles()]).then(
      ([{ canWrite, repositories }, { profiles }]) => {
        this.setState({
          canWrite,
          referencedProfiles: keyBy(profiles, 'key'),
          referencedRepositories: keyBy(repositories, 'key')
        });
        this.fetchFirstRules();
      },
      this.stopLoading
    );
  };

  makeFetchRequest = (query?: RawQuery) =>
    searchRules({ ...this.getSearchParameters(), ...query }).then(
      ({ actives: rawActives, facets: rawFacets, p, ps, rules, total }) => {
        const actives = rawActives && parseActives(rawActives);
        const facets = rawFacets && parseFacets(rawFacets);
        const paging = { pageIndex: p, pageSize: ps, total };
        return { actives, facets, paging, rules };
      }
    );

  fetchFirstRules = (query?: RawQuery) => {
    this.setState({ loading: true });
    this.makeFetchRequest(query).then(({ actives, facets, paging, rules }) => {
      if (this.mounted) {
        const openRule = this.getOpenRule(this.props, rules);
        const usingPermalink = hasRuleKey(this.props.location.query);
        const selected = rules.length > 0 ? (openRule && openRule.key) || rules[0].key : undefined;
        this.setState({
          actives,
          facets,
          loading: false,
          openRule,
          paging,
          rules,
          selected,
          usingPermalink
        });
      }
    }, this.stopLoading);
  };

  fetchMoreRules = () => {
    const { paging } = this.state;
    if (paging) {
      this.setState({ loading: true });
      const nextPage = paging.pageIndex + 1;
      this.makeFetchRequest({ p: nextPage, facets: undefined }).then(
        ({ actives, paging, rules }) => {
          if (this.mounted) {
            this.setState((state: State) => ({
              actives: { ...state.actives, ...actives },
              loading: false,
              paging,
              rules: [...state.rules, ...rules]
            }));
          }
        },
        this.stopLoading
      );
    }
  };

  fetchFacet = (facet: FacetKey) => {
    this.makeFetchRequest({ ps: 1, facets: getServerFacet(facet) }).then(({ facets }) => {
      if (this.mounted) {
        this.setState(state => ({ facets: { ...state.facets, ...facets }, loading: false }));
      }
    }, this.stopLoading);
  };

  getSelectedIndex = ({ selected, rules } = this.state) => {
    const index = rules.findIndex(rule => rule.key === selected);
    return index !== -1 ? index : undefined;
  };

  selectNextRule = () => {
    const { rules, loading, paging } = this.state;
    const selectedIndex = this.getSelectedIndex();
    if (selectedIndex !== undefined) {
      if (
        selectedIndex > rules.length - LIMIT_BEFORE_LOAD_MORE &&
        !loading &&
        paging &&
        rules.length < paging.total
      ) {
        this.fetchMoreRules();
      }
      if (rules && selectedIndex < rules.length - 1) {
        if (this.state.openRule) {
          this.openRule(rules[selectedIndex + 1].key);
        } else {
          this.setState({ selected: rules[selectedIndex + 1].key });
        }
      }
    }
  };

  selectPreviousRule = () => {
    const { rules } = this.state;
    const selectedIndex = this.getSelectedIndex();
    if (rules && selectedIndex !== undefined && selectedIndex > 0) {
      if (this.state.openRule) {
        this.openRule(rules[selectedIndex - 1].key);
      } else {
        this.setState({ selected: rules[selectedIndex - 1].key });
      }
    }
  };

  getRulePath = (rule: string) => ({
    pathname: this.props.location.pathname,
    query: { ...serializeQuery(this.state.query), open: rule }
  });

  openRule = (rule: string) => {
    const path = this.getRulePath(rule);
    if (this.state.openRule) {
      this.props.router.replace(path);
    } else {
      this.props.router.push(path);
    }
  };

  openSelectedRule = () => {
    const { selected } = this.state;
    if (selected) {
      this.openRule(selected);
    }
  };

  closeRule = () => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...serializeQuery(this.state.query),
        open: undefined
      }
    });
    this.scrollToSelectedRule(false);
  };

  scrollToSelectedRule = (smooth = true) => {
    const { selected } = this.state;
    if (selected) {
      const element = document.querySelector(`[data-rule="${selected}"]`);
      if (element) {
        scrollToElement(element, { topOffset: 150, bottomOffset: 100, smooth });
      }
    }
  };

  getRuleActivation = (rule: string) => {
    const { actives, query } = this.state;
    if (actives && actives[rule] && query.profile) {
      return actives[rule][query.profile];
    } else {
      return undefined;
    }
  };

  getSelectedProfile = () => {
    const { query, referencedProfiles } = this.state;
    if (query.profile) {
      return referencedProfiles[query.profile];
    } else {
      return undefined;
    }
  };

  closeFacet = (facet: string) =>
    this.setState(state => ({
      openFacets: { ...state.openFacets, [facet]: false }
    }));

  handleRuleOpen = (ruleKey: string) => {
    this.props.router.push(this.getRulePath(ruleKey));
  };

  handleBack = (event?: React.SyntheticEvent<HTMLAnchorElement>) => {
    const { usingPermalink } = this.state;

    if (event) {
      event.preventDefault();
      event.currentTarget.blur();
    }

    if (usingPermalink) {
      this.handleReset();
    } else {
      this.closeRule();
    }
  };

  handleFilterChange = (changes: Partial<Query>) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: serializeQuery({ ...this.state.query, ...changes })
    });

    this.setState(({ openFacets }) => ({
      openFacets: {
        ...openFacets,
        sonarsourceSecurity: shouldOpenSonarSourceSecurityFacet(openFacets, changes),
        standards: shouldOpenStandardsFacet(openFacets, changes)
      }
    }));
  };

  handleFacetToggle = (property: string) => {
    this.setState(state => {
      const willOpenProperty = !state.openFacets[property];
      const newState = {
        loading: state.loading,
        openFacets: { ...state.openFacets, [property]: willOpenProperty }
      };

      // Try to open sonarsource security "subfacet" by default if the standard facet is open
      if (willOpenProperty && property === STANDARDS) {
        newState.openFacets.sonarsourceSecurity = shouldOpenSonarSourceSecurityFacet(
          newState.openFacets,
          state.query
        );
        // Force loading of sonarsource security facet data
        property = newState.openFacets.sonarsourceSecurity ? 'sonarsourceSecurity' : property;
      }

      if (shouldRequestFacet(property) && (!state.facets || !state.facets[property])) {
        newState.loading = true;
        this.fetchFacet(property);
      }

      return newState;
    });
  };

  handleReload = () => this.fetchFirstRules();

  handleReset = () => this.props.router.push({ pathname: this.props.location.pathname });

  /** Tries to take rule by index, or takes the last one  */
  pickRuleAround = (rules: Rule[], selectedIndex: number | undefined) => {
    if (selectedIndex === undefined || rules.length === 0) {
      return undefined;
    }
    if (selectedIndex >= 0 && selectedIndex < rules.length) {
      return rules[selectedIndex].key;
    }
    return rules[rules.length - 1].key;
  };

  handleRuleDelete = (ruleKey: string) => {
    if (this.state.query.ruleKey === ruleKey) {
      this.handleReset();
    } else {
      this.setState(state => {
        const rules = state.rules.filter(rule => rule.key !== ruleKey);
        const selectedIndex = this.getSelectedIndex(state);
        const selected = this.pickRuleAround(rules, selectedIndex);
        return { rules, selected };
      });
      this.closeRule();
    }
  };

  handleRuleActivate = (profile: string, rule: string, activation: Activation) =>
    this.setState((state: State) => {
      const { actives = {} } = state;
      if (!actives[rule]) {
        return { actives: { ...actives, [rule]: { [profile]: activation } } };
      }

      return { actives: { ...actives, [rule]: { ...actives[rule], [profile]: activation } } };
    });

  handleRuleDeactivate = (profile: string, rule: string) =>
    this.setState(state => {
      const { actives } = state;
      if (actives && actives[rule]) {
        const newRule = { ...actives[rule] };
        delete newRule[profile];
        return { actives: { ...actives, [rule]: newRule } };
      }
      return null;
    });

  handleSearch = (searchQuery: string) => this.handleFilterChange({ searchQuery });

  isFiltered = () => Object.keys(serializeQuery(this.state.query)).length > 0;

  renderBulkButton = () => {
    const { currentUser } = this.props;
    const { canWrite, paging, query, referencedProfiles } = this.state;
    const canUpdate = canWrite || Object.values(referencedProfiles).some(p => p.actions?.edit);

    if (!isLoggedIn(currentUser) || !canUpdate) {
      return <div />;
    }

    return (
      paging && (
        <BulkChange query={query} referencedProfiles={referencedProfiles} total={paging.total} />
      )
    );
  };

  render() {
    const { paging, rules } = this.state;
    const selectedIndex = this.getSelectedIndex();

    return (
      <>
        <Suggestions suggestions="coding_rules" />
        <Helmet defer={false} title={translate('coding_rules.page')}>
          <meta content="noindex" name="robots" />
        </Helmet>
        <div className="layout-page" id="coding-rules-page">
          <ScreenPositionHelper className="layout-page-side-outer">
            {({ top }) => (
              <div className="layout-page-side" style={{ top }}>
                <div className="layout-page-side-inner">
                  <div className="layout-page-filters">
                    <A11ySkipTarget
                      anchor="rules_filters"
                      label={translate('coding_rules.skip_to_filters')}
                      weight={10}
                    />
                    <FiltersHeader displayReset={this.isFiltered()} onReset={this.handleReset} />
                    <SearchBox
                      className="spacer-bottom"
                      id="coding-rules-search"
                      maxLength={MAX_SEARCH_LENGTH}
                      minLength={2}
                      onChange={this.handleSearch}
                      placeholder={translate('search.search_for_rules')}
                      value={this.state.query.searchQuery || ''}
                    />
                    <FacetsList
                      facets={this.state.facets}
                      onFacetToggle={this.handleFacetToggle}
                      onFilterChange={this.handleFilterChange}
                      openFacets={this.state.openFacets}
                      query={this.state.query}
                      referencedProfiles={this.state.referencedProfiles}
                      referencedRepositories={this.state.referencedRepositories}
                      selectedProfile={this.getSelectedProfile()}
                    />
                  </div>
                </div>
              </div>
            )}
          </ScreenPositionHelper>

          <div className="layout-page-main">
            <div className="layout-page-header-panel layout-page-main-header">
              <div className="layout-page-header-panel-inner layout-page-main-header-inner">
                <div className="layout-page-main-inner">
                  <A11ySkipTarget anchor="rules_main" />
                  <div className="display-flex-space-between">
                    {this.state.openRule ? (
                      <a
                        className="js-back display-inline-flex-center link-no-underline"
                        href="#"
                        onClick={this.handleBack}>
                        <BackIcon className="spacer-right" />
                        {this.state.usingPermalink
                          ? translate('coding_rules.see_all')
                          : translate('coding_rules.return_to_list')}
                      </a>
                    ) : (
                      this.renderBulkButton()
                    )}
                    {!this.state.usingPermalink && (
                      <PageActions
                        loading={this.state.loading}
                        onReload={this.handleReload}
                        paging={paging}
                        selectedIndex={selectedIndex}
                      />
                    )}
                  </div>
                </div>
              </div>
            </div>

            <div className="layout-page-main-inner">
              {this.state.openRule ? (
                <RuleDetails
                  allowCustomRules={true}
                  canWrite={this.state.canWrite}
                  onActivate={this.handleRuleActivate}
                  onDeactivate={this.handleRuleDeactivate}
                  onDelete={this.handleRuleDelete}
                  onFilterChange={this.handleFilterChange}
                  referencedProfiles={this.state.referencedProfiles}
                  referencedRepositories={this.state.referencedRepositories}
                  ruleKey={this.state.openRule.key}
                  selectedProfile={this.getSelectedProfile()}
                />
              ) : (
                <>
                  {rules.map(rule => (
                    <RuleListItem
                      activation={this.getRuleActivation(rule.key)}
                      canWrite={this.state.canWrite}
                      isLoggedIn={isLoggedIn(this.props.currentUser)}
                      key={rule.key}
                      onActivate={this.handleRuleActivate}
                      onDeactivate={this.handleRuleDeactivate}
                      onFilterChange={this.handleFilterChange}
                      onOpen={this.handleRuleOpen}
                      rule={rule}
                      selected={rule.key === this.state.selected}
                      selectedProfile={this.getSelectedProfile()}
                    />
                  ))}
                  {paging !== undefined && (
                    <ListFooter
                      count={rules.length}
                      loadMore={this.fetchMoreRules}
                      ready={!this.state.loading}
                      total={paging.total}
                    />
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </>
    );
  }
}

function parseActives(rawActives: Dict<RuleActivation[]>) {
  const actives: Actives = {};
  for (const [rule, activations] of Object.entries(rawActives)) {
    actives[rule] = {};
    for (const { inherit, qProfile, severity } of activations) {
      actives[rule][qProfile] = { inherit, severity };
    }
  }
  return actives;
}

function parseFacets(rawFacets: { property: string; values: { count: number; val: string }[] }[]) {
  const facets: Facets = {};
  for (const rawFacet of rawFacets) {
    const values: Dict<number> = {};
    for (const rawValue of rawFacet.values) {
      values[rawValue.val] = rawValue.count;
    }
    facets[getAppFacet(rawFacet.property)] = values;
  }
  return facets;
}

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state)
});

export default withRouter(connect(mapStateToProps)(App));
