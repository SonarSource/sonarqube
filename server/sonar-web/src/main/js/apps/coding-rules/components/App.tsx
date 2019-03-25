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
import { Helmet } from 'react-helmet';
import { connect } from 'react-redux';
import { withRouter, WithRouterProps } from 'react-router';
import * as key from 'keymaster';
import { keyBy } from 'lodash';
import BulkChange from './BulkChange';
import FacetsList from './FacetsList';
import PageActions from './PageActions';
import RuleDetails from './RuleDetails';
import RuleListItem from './RuleListItem';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import {
  Facets,
  Query,
  parseQuery,
  serializeQuery,
  areQueriesEqual,
  shouldRequestFacet,
  FacetKey,
  OpenFacets,
  getServerFacet,
  getAppFacet,
  Actives,
  Activation,
  getOpen
} from '../query';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import ListFooter from '../../../components/controls/ListFooter';
import FiltersHeader from '../../../components/common/FiltersHeader';
import SearchBox from '../../../components/controls/SearchBox';
import { searchRules, getRulesApp } from '../../../api/rules';
import { searchQualityProfiles, Profile } from '../../../api/quality-profiles';
import {
  getCurrentUser,
  getLanguages,
  getMyOrganizations,
  Store,
  getAppState
} from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import { hasPrivateAccess } from '../../../helpers/organizations';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass
} from '../../../helpers/pages';
import { RawQuery } from '../../../helpers/query';
import { scrollToElement } from '../../../helpers/scrolling';
import '../../../components/search-navigator.css';
import '../styles.css';

const PAGE_SIZE = 100;
const LIMIT_BEFORE_LOAD_MORE = 5;

interface StateToProps {
  appState: T.AppState;
  currentUser: T.CurrentUser;
  languages: T.Languages;
  userOrganizations: T.Organization[];
}

interface OwnProps extends WithRouterProps {
  organization: T.Organization | undefined;
}

type Props = OwnProps & StateToProps;

interface State {
  actives?: Actives;
  canWrite?: boolean;
  facets?: Facets;
  loading: boolean;
  openFacets: OpenFacets;
  openRule?: T.Rule;
  paging?: T.Paging;
  query: Query;
  referencedProfiles: T.Dict<Profile>;
  referencedRepositories: T.Dict<{ key: string; language: string; name: string }>;
  rules: T.Rule[];
  selected?: string;
}

export class App extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      openFacets: { languages: true, types: true },
      query: parseQuery(props.location.query),
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
      // or if he went from the source code back to the list of issues
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
      this.closeRule();
      return false;
    });
  };

  detachShortcuts = () => key.deleteScope('coding-rules');

  getOpenRule = (props: Props, rules: T.Rule[]) => {
    const open = getOpen(props.location.query);
    return open && rules.find(rule => rule.key === open);
  };

  getFacetsToFetch = () =>
    Object.keys(this.state.openFacets)
      .filter((facet: FacetKey) => this.state.openFacets[facet])
      .filter((facet: FacetKey) => shouldRequestFacet(facet))
      .map((facet: FacetKey) => getServerFacet(facet));

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
    organization: this.props.organization && this.props.organization.key,
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
    const organization = this.props.organization && this.props.organization.key;
    Promise.all([getRulesApp({ organization }), this.fetchQualityProfiles()]).then(
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
        const selected = rules.length > 0 ? (openRule && openRule.key) || rules[0].key : undefined;
        this.setState({ actives, facets, loading: false, openRule, paging, rules, selected });
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
    this.setState({ loading: true });
    this.makeFetchRequest({ ps: 1, facets: getServerFacet(facet) }).then(({ facets }) => {
      if (this.mounted) {
        this.setState(state => ({ facets: { ...state.facets, ...facets }, loading: false }));
      }
    }, this.stopLoading);
  };

  fetchQualityProfiles = () => {
    const { currentUser, organization, userOrganizations } = this.props;
    if (hasPrivateAccess(currentUser, organization, userOrganizations)) {
      return searchQualityProfiles({ organization: organization && organization.key });
    }
    return { profiles: [] };
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

  handleBack = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.closeRule();
  };

  handleFilterChange = (changes: Partial<Query>) =>
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: serializeQuery({ ...this.state.query, ...changes })
    });

  handleFacetToggle = (facet: keyof Query) => {
    this.setState(state => ({
      openFacets: { ...state.openFacets, [facet]: !state.openFacets[facet] }
    }));
    if (shouldRequestFacet(facet) && (!this.state.facets || !this.state.facets[facet])) {
      this.fetchFacet(facet);
    }
  };

  handleReload = () => this.fetchFirstRules();

  handleReset = () => this.props.router.push({ pathname: this.props.location.pathname });

  /** Tries to take rule by index, or takes the last one  */
  pickRuleAround = (rules: T.Rule[], selectedIndex: number | undefined) => {
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

  render() {
    const { paging, rules } = this.state;
    const selectedIndex = this.getSelectedIndex();
    const organization = this.props.organization && this.props.organization.key;
    const hideQualityProfiles = !hasPrivateAccess(
      this.props.currentUser,
      this.props.organization,
      this.props.userOrganizations
    );
    return (
      <>
        <Suggestions suggestions="coding_rules" />
        <Helmet title={translate('coding_rules.page')}>
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
                      minLength={2}
                      onChange={this.handleSearch}
                      placeholder={translate('search.search_for_rules')}
                      value={this.state.query.searchQuery || ''}
                    />
                    <FacetsList
                      facets={this.state.facets}
                      hideProfileFacet={hideQualityProfiles}
                      onFacetToggle={this.handleFacetToggle}
                      onFilterChange={this.handleFilterChange}
                      openFacets={this.state.openFacets}
                      organization={organization}
                      organizationsEnabled={this.props.appState.organizationsEnabled}
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
                  {this.state.openRule ? (
                    <a className="js-back" href="#" onClick={this.handleBack}>
                      {translate('coding_rules.return_to_list')}
                    </a>
                  ) : (
                    this.state.paging && (
                      <BulkChange
                        languages={this.props.languages}
                        organization={organization}
                        query={this.state.query}
                        referencedProfiles={this.state.referencedProfiles}
                        total={this.state.paging.total}
                      />
                    )
                  )}
                  <PageActions
                    loading={this.state.loading}
                    onReload={this.handleReload}
                    paging={paging}
                    selectedIndex={selectedIndex}
                  />
                </div>
              </div>
            </div>

            <div className="layout-page-main-inner">
              {this.state.openRule ? (
                <RuleDetails
                  allowCustomRules={!this.props.appState.organizationsEnabled}
                  canWrite={this.state.canWrite}
                  hideQualityProfiles={hideQualityProfiles}
                  onActivate={this.handleRuleActivate}
                  onDeactivate={this.handleRuleDeactivate}
                  onDelete={this.handleRuleDelete}
                  onFilterChange={this.handleFilterChange}
                  organization={organization}
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
                      key={rule.key}
                      onActivate={this.handleRuleActivate}
                      onDeactivate={this.handleRuleDeactivate}
                      onFilterChange={this.handleFilterChange}
                      onOpen={this.handleRuleOpen}
                      organization={organization}
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

function parseActives(rawActives: T.Dict<T.RuleActivation[]>) {
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
    const values: T.Dict<number> = {};
    for (const rawValue of rawFacet.values) {
      values[rawValue.val] = rawValue.count;
    }
    facets[getAppFacet(rawFacet.property)] = values;
  }
  return facets;
}

const mapStateToProps = (state: Store) => ({
  appState: getAppState(state),
  currentUser: getCurrentUser(state),
  languages: getLanguages(state),
  userOrganizations: getMyOrganizations(state)
});

export default withRouter(connect(mapStateToProps)(App));
