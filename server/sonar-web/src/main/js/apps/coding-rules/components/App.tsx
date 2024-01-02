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
import { keyBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Profile, searchQualityProfiles } from '../../../api/quality-profiles';
import { getRulesApp, searchRules } from '../../../api/rules';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import FiltersHeader from '../../../components/common/FiltersHeader';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import ListFooter from '../../../components/controls/ListFooter';
import SearchBox from '../../../components/controls/SearchBox';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import BackIcon from '../../../components/icons/BackIcon';
import '../../../components/search-navigator.css';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass,
} from '../../../helpers/pages';
import { SecurityStandard } from '../../../types/security';
import { Dict, Paging, RawQuery, Rule, RuleActivation } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import {
  shouldOpenSonarSourceSecurityFacet,
  shouldOpenStandardsChildFacet,
  shouldOpenStandardsFacet,
  STANDARDS,
} from '../../issues/utils';
import {
  Activation,
  Actives,
  areQueriesEqual,
  FacetKey,
  Facets,
  getAppFacet,
  getOpen,
  getSelected,
  getServerFacet,
  hasRuleKey,
  OpenFacets,
  parseQuery,
  Query,
  serializeQuery,
  shouldRequestFacet,
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

interface Props {
  currentUser: CurrentUser;
  location: Location;
  router: Router;
}

interface State {
  actives?: Actives;
  canWrite?: boolean;
  facets?: Facets;
  loading: boolean;
  openFacets: OpenFacets;
  paging?: Paging;
  referencedProfiles: Dict<Profile>;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  rules: Rule[];
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
        'owaspTop10-2021': shouldOpenStandardsChildFacet(
          {},
          query,
          SecurityStandard.OWASP_TOP10_2021
        ),
        sansTop25: shouldOpenStandardsChildFacet({}, query, SecurityStandard.SANS_TOP25),
        sonarsourceSecurity: shouldOpenSonarSourceSecurityFacet({}, query),
        standards: shouldOpenStandardsFacet({}, query),
        types: true,
      },
      referencedProfiles: {},
      referencedRepositories: {},
      rules: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
    addWhitePageClass();
    addSideBarClass();
    this.attachShortcuts();
    this.fetchInitialData();
  }

  componentDidUpdate(prevProps: Props) {
    if (!areQueriesEqual(prevProps.location.query, this.props.location.query)) {
      this.fetchFirstRules();
    }
    if (this.getSelectedRuleKey(prevProps) !== this.getSelectedRuleKey(this.props)) {
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
    document.addEventListener('keydown', this.handleKeyPress);
  };

  handleKeyPress = (event: KeyboardEvent) => {
    if (isInput(event) || isShortcut(event)) {
      return true;
    }
    switch (event.key) {
      case KeyboardKeys.LeftArrow:
        event.preventDefault();
        this.handleBack();
        break;
      case KeyboardKeys.RightArrow:
        event.preventDefault();
        this.openSelectedRule();
        break;
      case KeyboardKeys.DownArrow:
        event.preventDefault();
        this.selectNextRule();
        break;
      case KeyboardKeys.UpArrow:
        event.preventDefault();
        this.selectPreviousRule();
        break;
    }
  };

  detachShortcuts = () => {
    document.removeEventListener('keydown', this.handleKeyPress);
  };

  getOpenRule = (rules: Rule[]) => {
    const open = getOpen(this.props.location.query);
    return open && rules.find((rule) => rule.key === open);
  };

  getSelectedRuleKey = (props: Props) => {
    return getSelected(props.location.query);
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
      'templateKey',
    ];
    if (parseQuery(this.props.location.query).profile) {
      fields.push('actives', 'params');
    }
    return fields;
  };

  getSearchParameters = () => ({
    f: this.getFieldsToFetch().join(),
    facets: this.getFacetsToFetch().join(),
    ps: PAGE_SIZE,
    s: 'name',
    ...this.props.location.query,
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
          referencedRepositories: keyBy(repositories, 'key'),
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
        const openRule = this.getOpenRule(rules);
        const selected = rules.length > 0 && !openRule ? rules[0].key : undefined;
        this.routeSelectedRulePath(selected);
        this.setState({
          actives,
          facets,
          loading: false,
          paging,
          rules,
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
              rules: [...state.rules, ...rules],
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
        this.setState((state) => ({ facets: { ...state.facets, ...facets }, loading: false }));
      }
    }, this.stopLoading);
  };

  getSelectedIndex = ({ rules } = this.state) => {
    const selected = this.getSelectedRuleKey(this.props) || getOpen(this.props.location.query);
    const index = rules.findIndex((rule) => rule.key === selected);
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
        if (this.getOpenRule(this.state.rules)) {
          this.openRule(rules[selectedIndex + 1].key);
        } else {
          this.routeSelectedRulePath(rules[selectedIndex + 1].key);
        }
      }
    }
  };

  selectPreviousRule = () => {
    const { rules } = this.state;
    const selectedIndex = this.getSelectedIndex();
    if (rules && selectedIndex !== undefined && selectedIndex > 0) {
      if (this.getOpenRule(this.state.rules)) {
        this.openRule(rules[selectedIndex - 1].key);
      } else {
        this.routeSelectedRulePath(rules[selectedIndex - 1].key);
      }
    }
  };

  getRulePath = (rule: string) => ({
    pathname: this.props.location.pathname,
    query: {
      ...serializeQuery(parseQuery(this.props.location.query)),
      open: rule,
    },
  });

  routeSelectedRulePath = (rule?: string) => {
    if (rule) {
      this.props.router.replace({
        pathname: this.props.location.pathname,
        query: { ...serializeQuery(parseQuery(this.props.location.query)), selected: rule },
      });
    }
  };

  openRule = (rule: string) => {
    const path = this.getRulePath(rule);
    if (this.getOpenRule(this.state.rules)) {
      this.props.router.replace(path);
    } else {
      this.props.router.push(path);
    }
  };

  openSelectedRule = () => {
    const selected = this.getSelectedRuleKey(this.props);
    if (selected) {
      this.openRule(selected);
    }
  };

  closeRule = () => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...serializeQuery(parseQuery(this.props.location.query)),
        selected: this.getOpenRule(this.state.rules)?.key || this.getSelectedRuleKey(this.props),
        open: undefined,
      },
    });
    this.scrollToSelectedRule();
  };

  scrollToSelectedRule = () => {
    const selected = this.getSelectedRuleKey(this.props);
    if (selected) {
      const element = document.querySelector(`[data-rule="${selected}"]`);
      if (element) {
        element.scrollIntoView({ behavior: 'auto', block: 'center' });
      }
    }
  };

  getRuleActivation = (rule: string) => {
    const { actives } = this.state;
    const query = parseQuery(this.props.location.query);
    if (actives && actives[rule] && query.profile) {
      return actives[rule][query.profile];
    }
  };

  getSelectedProfile = () => {
    const { referencedProfiles } = this.state;
    const query = parseQuery(this.props.location.query);
    if (query.profile) {
      return referencedProfiles[query.profile];
    }
  };

  closeFacet = (facet: string) =>
    this.setState((state) => ({
      openFacets: { ...state.openFacets, [facet]: false },
    }));

  handleRuleOpen = (ruleKey: string) => {
    this.props.router.push(this.getRulePath(ruleKey));
  };

  handleBack = (event?: React.SyntheticEvent<HTMLAnchorElement>) => {
    const usingPermalink = hasRuleKey(this.props.location.query);

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
      query: serializeQuery({ ...parseQuery(this.props.location.query), ...changes }),
    });

    this.setState(({ openFacets }) => ({
      openFacets: {
        ...openFacets,
        sonarsourceSecurity: shouldOpenSonarSourceSecurityFacet(openFacets, changes),
        standards: shouldOpenStandardsFacet(openFacets, changes),
      },
    }));
  };

  handleFacetToggle = (property: string) => {
    this.setState((state) => {
      const willOpenProperty = !state.openFacets[property];
      const newState = {
        loading: state.loading,
        openFacets: { ...state.openFacets, [property]: willOpenProperty },
      };

      // Try to open sonarsource security "subfacet" by default if the standard facet is open
      if (willOpenProperty && property === STANDARDS) {
        newState.openFacets.sonarsourceSecurity = shouldOpenSonarSourceSecurityFacet(
          newState.openFacets,
          parseQuery(this.props.location.query)
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
    if (parseQuery(this.props.location.query).ruleKey === ruleKey) {
      this.handleReset();
    } else {
      this.setState((state) => {
        const rules = state.rules.filter((rule) => rule.key !== ruleKey);
        const selectedIndex = this.getSelectedIndex(state);
        const selected = this.pickRuleAround(rules, selectedIndex);
        this.routeSelectedRulePath(selected);
        return { rules };
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
    this.setState((state) => {
      const { actives } = state;
      if (actives && actives[rule]) {
        const newRule = { ...actives[rule] };
        delete newRule[profile];
        return { actives: { ...actives, [rule]: newRule } };
      }
      return null;
    });

  handleSearch = (searchQuery: string) => this.handleFilterChange({ searchQuery });

  isFiltered = () => Object.keys(serializeQuery(parseQuery(this.props.location.query))).length > 0;

  renderBulkButton = () => {
    const { currentUser } = this.props;
    const { canWrite, paging, referencedProfiles } = this.state;
    const query = parseQuery(this.props.location.query);
    const canUpdate = canWrite || Object.values(referencedProfiles).some((p) => p.actions?.edit);

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
    const query = parseQuery(this.props.location.query);
    const openRule = this.getOpenRule(this.state.rules);
    const usingPermalink = hasRuleKey(this.props.location.query);
    const selected = this.getSelectedRuleKey(this.props);

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
                      value={query.searchQuery || ''}
                    />
                    <FacetsList
                      facets={this.state.facets}
                      onFacetToggle={this.handleFacetToggle}
                      onFilterChange={this.handleFilterChange}
                      openFacets={this.state.openFacets}
                      query={query}
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
                    {openRule ? (
                      <a
                        className="js-back display-inline-flex-center link-no-underline"
                        href="#"
                        onClick={this.handleBack}
                      >
                        <BackIcon className="spacer-right" />
                        {usingPermalink
                          ? translate('coding_rules.see_all')
                          : translate('coding_rules.return_to_list')}
                      </a>
                    ) : (
                      this.renderBulkButton()
                    )}
                    {!usingPermalink && (
                      <PageActions paging={paging} selectedIndex={selectedIndex} />
                    )}
                  </div>
                </div>
              </div>
            </div>

            <div className="layout-page-main-inner">
              {openRule ? (
                <RuleDetails
                  allowCustomRules={true}
                  canWrite={this.state.canWrite}
                  onActivate={this.handleRuleActivate}
                  onDeactivate={this.handleRuleDeactivate}
                  onDelete={this.handleRuleDelete}
                  onFilterChange={this.handleFilterChange}
                  referencedProfiles={this.state.referencedProfiles}
                  referencedRepositories={this.state.referencedRepositories}
                  ruleKey={openRule.key}
                  selectedProfile={this.getSelectedProfile()}
                />
              ) : (
                <>
                  <ul>
                    {rules.map((rule) => (
                      <RuleListItem
                        activation={this.getRuleActivation(rule.key)}
                        isLoggedIn={isLoggedIn(this.props.currentUser)}
                        key={rule.key}
                        onActivate={this.handleRuleActivate}
                        onDeactivate={this.handleRuleDeactivate}
                        onFilterChange={this.handleFilterChange}
                        onOpen={this.handleRuleOpen}
                        rule={rule}
                        selected={rule.key === selected}
                        selectedProfile={this.getSelectedProfile()}
                      />
                    ))}
                  </ul>
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

export default withRouter(withCurrentUserContext(App));
