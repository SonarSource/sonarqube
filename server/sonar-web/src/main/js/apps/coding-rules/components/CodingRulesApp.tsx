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

import styled from '@emotion/styled';
import {
  InputSearch,
  LAYOUT_FOOTER_HEIGHT,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LargeCenteredLayout,
  PageContentFontWrapper,
  themeBorder,
  themeColor,
} from 'design-system';
import { keyBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location, RawQuery, Router } from '~sonar-aligned/types/router';
import { Profile, searchQualityProfiles } from '../../../api/quality-profiles';
import { getRulesApp, searchRules } from '../../../api/rules';
import { getValue } from '../../../api/settings';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import ListFooter from '../../../components/controls/ListFooter';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import '../../../components/search-navigator.css';
import { DocLink } from '../../../helpers/doc-links';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { SecurityStandard } from '../../../types/security';
import { SettingsKey } from '../../../types/settings';
import { Dict, Paging, Rule, RuleActivation } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';
import { FiltersHeader } from '../../issues/sidebar/FiltersHeader';
import {
  STANDARDS,
  shouldOpenSonarSourceSecurityFacet,
  shouldOpenStandardsChildFacet,
  shouldOpenStandardsFacet,
} from '../../issues/utils';
import {
  Actives,
  FacetKey,
  Facets,
  OpenFacets,
  Query,
  areQueriesEqual,
  getOpen,
  getSelected,
  hasRuleKey,
  parseQuery,
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
  canDeactivateInherited?: boolean;
  canWrite?: boolean;
  facets?: Facets;
  loading: boolean;
  openFacets: OpenFacets;
  paging?: Paging;
  referencedProfiles: Dict<Profile>;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  rules: Rule[];
}

const RULE_LIST_HEADER_HEIGHT = 68;

export class CodingRulesApp extends React.PureComponent<Props, State> {
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
          SecurityStandard.OWASP_TOP10_2021,
        ),
        sonarsourceSecurity: shouldOpenSonarSourceSecurityFacet({}, query),
        standards: shouldOpenStandardsFacet({}, query),
        cleanCodeAttributeCategories: true,
        impactSoftwareQualities: true,
      },
      referencedProfiles: {},
      referencedRepositories: {},
      rules: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
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
    this.detachShortcuts();
  }

  attachShortcuts = () => {
    document.addEventListener('keydown', this.handleKeyDown);
  };

  handleKeyDown = (event: KeyboardEvent) => {
    if (isInput(event) || isShortcut(event)) {
      return;
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
    document.removeEventListener('keydown', this.handleKeyDown);
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
      .filter((facet: FacetKey) => shouldRequestFacet(facet));
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
      'cleanCodeAttribute',
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
    ...serializeQuery(parseQuery(this.props.location.query)),
  });

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchInitialData = () => {
    this.setState({ loading: true });

    Promise.all([
      getRulesApp(),
      searchQualityProfiles(),
      getValue({ key: SettingsKey.QPAdminCanDisableInheritedRules }),
    ]).then(([{ canWrite, repositories }, { profiles }, setting]) => {
      this.setState({
        canWrite,
        canDeactivateInherited: setting?.value === 'true',
        referencedProfiles: keyBy(profiles, 'key'),
        referencedRepositories: keyBy(repositories, 'key'),
      });
      this.fetchFirstRules();
    }, this.stopLoading);
  };

  makeFetchRequest = (query?: RawQuery) =>
    searchRules({ ...this.getSearchParameters(), ...query }).then(
      ({ actives: rawActives, facets: rawFacets, rules, paging }) => {
        const actives = rawActives && parseActives(rawActives);
        const facets = rawFacets && parseFacets(rawFacets);
        return { actives, facets, paging, rules };
      },
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
        this.stopLoading,
      );
    }
  };

  fetchFacet = (facet: FacetKey) => {
    this.makeFetchRequest({ ps: 1, facets: facet }).then(({ facets }) => {
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

  handleSelectRule = (key: string) => {
    this.routeSelectedRulePath(key);
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

    return actives?.[rule] && query.profile ? actives[rule][query.profile] : undefined;
  };

  getSelectedProfile = () => {
    const { referencedProfiles } = this.state;
    const query = parseQuery(this.props.location.query);

    return query.profile ? referencedProfiles[query.profile] : undefined;
  };

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
          parseQuery(this.props.location.query),
        );
        // Force loading of sonarsource security facet data
        property = newState.openFacets.sonarsourceSecurity ? 'sonarsourceSecurity' : property;
      }

      if (shouldRequestFacet(property) && !state.facets?.[property]) {
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

  handleRuleActivate = (profile: string, rule: string, activation: RuleActivation) =>
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
      if (actives?.[rule]) {
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
    const { canWrite = false, paging, referencedProfiles } = this.state;
    const query = parseQuery(this.props.location.query);
    const canUpdate = canWrite || Object.values(referencedProfiles).some((p) => p.actions?.edit);

    if (!isLoggedIn(currentUser) || !canUpdate) {
      return <div />;
    }

    return (
      paging && (
        <BulkChange
          onSubmit={this.handleReload}
          query={query}
          referencedProfiles={referencedProfiles}
          total={paging.total}
        />
      )
    );
  };

  render() {
    const { paging, rules } = this.state;
    const query = parseQuery(this.props.location.query);
    const openRule = this.getOpenRule(this.state.rules);
    const usingPermalink = hasRuleKey(this.props.location.query);
    const selected = this.getSelectedRuleKey(this.props);

    return (
      <>
        <Suggestions suggestion={DocLink.InstanceAdminQualityProfiles} />
        {openRule ? (
          <Helmet
            defer={false}
            title={translateWithParameters('coding_rule.page', openRule.langName, openRule.name)}
            titleTemplate={translateWithParameters(
              'page_title.template.with_category',
              translate('coding_rules.page'),
            )}
          />
        ) : (
          <Helmet defer={false} title={translate('coding_rules.page')}>
            <meta content="noindex" name="robots" />
          </Helmet>
        )}
        <LargeCenteredLayout id="coding-rules-page">
          <PageContentFontWrapper className="sw-typo-default">
            <div className="sw-grid sw-gap-x-12 sw-gap-y-6 sw-grid-cols-12 sw-w-full">
              <StyledContentWrapper
                as="nav"
                className="sw-col-span-3 sw-p-4 sw-overflow-y-auto"
                aria-label={translate('filters')}
                style={{
                  height: `calc(100vh - ${LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_FOOTER_HEIGHT}px)`,
                }}
              >
                <div>
                  <A11ySkipTarget
                    anchor="rules_filters"
                    label={translate('coding_rules.skip_to_filters')}
                    weight={10}
                  />
                  <FiltersHeader displayReset={this.isFiltered()} onReset={this.handleReset} />
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
              </StyledContentWrapper>

              <main className="sw-col-span-9">
                {!openRule && (
                  <div>
                    <A11ySkipTarget anchor="rules_main" />

                    <div className="sw-flex sw-justify-between sw-py-4">
                      <InputSearch
                        className="sw-min-w-abs-250 sw-max-w-abs-350 sw-mr-4"
                        id="coding-rules-search"
                        maxLength={MAX_SEARCH_LENGTH}
                        minLength={2}
                        onChange={this.handleSearch}
                        placeholder={translate('search.search_for_rules')}
                        value={query.searchQuery ?? ''}
                        size="auto"
                      />
                      {this.renderBulkButton()}
                      {!usingPermalink && <PageActions paging={paging} />}
                    </div>
                  </div>
                )}

                <div
                  className="sw-overflow-y-auto"
                  style={{
                    height: `calc(100vh - ${LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_FOOTER_HEIGHT}px - ${
                      !openRule ? RULE_LIST_HEADER_HEIGHT : 0
                    }px)`,
                  }}
                >
                  {openRule ? (
                    <RuleDetails
                      allowCustomRules
                      canWrite={this.state.canWrite}
                      canDeactivateInherited={this.state.canDeactivateInherited}
                      onActivate={this.handleRuleActivate}
                      onDeactivate={this.handleRuleDeactivate}
                      onDelete={this.handleRuleDelete}
                      referencedProfiles={this.state.referencedProfiles}
                      referencedRepositories={this.state.referencedRepositories}
                      ruleKey={openRule.key}
                      selectedProfile={this.getSelectedProfile()}
                    />
                  ) : (
                    <>
                      <ul aria-label={translate('list_of_rules')}>
                        {rules.map((rule) => (
                          <RuleListItem
                            activation={this.getRuleActivation(rule.key)}
                            isLoggedIn={isLoggedIn(this.props.currentUser)}
                            canDeactivateInherited={this.state.canDeactivateInherited}
                            key={rule.key}
                            onActivate={this.handleRuleActivate}
                            onDeactivate={this.handleRuleDeactivate}
                            onOpen={this.handleRuleOpen}
                            rule={rule}
                            selected={rule.key === selected}
                            selectRule={this.handleSelectRule}
                            selectedProfile={this.getSelectedProfile()}
                          />
                        ))}
                      </ul>
                      {paging !== undefined && (
                        <ListFooter
                          className="sw-mb-4"
                          count={rules.length}
                          loadMore={this.fetchMoreRules}
                          ready={!this.state.loading}
                          total={paging.total}
                        />
                      )}
                    </>
                  )}
                </div>
              </main>
            </div>
          </PageContentFontWrapper>
        </LargeCenteredLayout>
      </>
    );
  }
}

function parseActives(rawActives: Dict<RuleActivation[]>) {
  const actives: Actives = {};
  for (const [rule, activations] of Object.entries(rawActives)) {
    actives[rule] = {};
    for (const activation of activations) {
      actives[rule][activation.qProfile] = { ...activation };
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
    facets[rawFacet.property as FacetKey] = values;
  }
  return facets;
}

export default withRouter(withCurrentUserContext(CodingRulesApp));

const StyledContentWrapper = styled.div`
  box-sizing: border-box;
  background-color: ${themeColor('filterbar')};
  border: ${themeBorder('default', 'filterbarBorder')};
  border-bottom: none;
  overflow-x: hidden;
`;
