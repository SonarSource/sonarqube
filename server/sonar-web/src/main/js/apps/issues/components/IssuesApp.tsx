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
import classNames from 'classnames';
import { debounce, keyBy, omit, without } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { searchIssues } from '../../../api/issues';
import { getRuleDetails } from '../../../api/rules';
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { PageContext } from '../../../app/components/indexation/PageUnavailableDueToIndexation';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import EmptySearch from '../../../components/common/EmptySearch';
import FiltersHeader from '../../../components/common/FiltersHeader';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import { Button } from '../../../components/controls/buttons';
import ButtonToggle from '../../../components/controls/ButtonToggle';
import Checkbox from '../../../components/controls/Checkbox';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import ListFooter from '../../../components/controls/ListFooter';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import withIndexationGuard from '../../../components/hoc/withIndexationGuard';
import { Location, Router, withRouter } from '../../../components/hoc/withRouter';
import RuleTabViewer from '../../../components/rules/RuleTabViewer';
import '../../../components/search-navigator.css';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import {
  fillBranchLike,
  getBranchLikeQuery,
  isPullRequest,
  isSameBranchLike,
} from '../../../helpers/branch-like';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { parseIssueFromResponse } from '../../../helpers/issues';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import {
  addSideBarClass,
  addWhitePageClass,
  removeSideBarClass,
  removeWhitePageClass,
} from '../../../helpers/pages';
import { serializeDate } from '../../../helpers/query';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier, isPortfolioLike } from '../../../types/component';
import {
  Facet,
  FetchIssuesPromise,
  ReferencedComponent,
  ReferencedLanguage,
  ReferencedRule,
} from '../../../types/issues';
import { SecurityStandard } from '../../../types/security';
import { Component, Dict, Issue, Paging, RawQuery, RuleDetails } from '../../../types/types';
import { CurrentUser, UserBase } from '../../../types/users';
import * as actions from '../actions';
import ConciseIssuesList from '../conciseIssuesList/ConciseIssuesList';
import ConciseIssuesListHeader from '../conciseIssuesList/ConciseIssuesListHeader';
import Sidebar from '../sidebar/Sidebar';
import '../styles.css';
import {
  areMyIssuesSelected,
  areQueriesEqual,
  getOpen,
  getOpenIssue,
  parseFacets,
  parseQuery,
  Query,
  saveMyIssues,
  serializeQuery,
  shouldOpenSonarSourceSecurityFacet,
  shouldOpenStandardsChildFacet,
  shouldOpenStandardsFacet,
  STANDARDS,
} from '../utils';
import BulkChangeModal, { MAX_PAGE_SIZE } from './BulkChangeModal';
import IssueHeader from './IssueHeader';
import IssuesList from './IssuesList';
import IssuesSourceViewer from './IssuesSourceViewer';
import NoIssues from './NoIssues';
import NoMyIssues from './NoMyIssues';
import PageActions from './PageActions';

interface Props {
  branchLike?: BranchLike;
  component?: Component;
  currentUser: CurrentUser;
  fetchBranchStatus: (branchLike: BranchLike, projectKey: string) => void;
  location: Location;
  router: Router;
}

export interface State {
  bulkChangeModal: boolean;
  cannotShowOpenIssue?: boolean;
  checkAll?: boolean;
  checked: string[];
  effortTotal?: number;
  facets: Dict<Facet>;
  issues: Issue[];
  loading: boolean;
  loadingRule: boolean;
  loadingFacets: Dict<boolean>;
  loadingMore: boolean;
  locationsNavigator: boolean;
  myIssues: boolean;
  openFacets: Dict<boolean>;
  openIssue?: Issue;
  openPopup?: { issue: string; name: string };
  openRuleDetails?: RuleDetails;
  paging?: Paging;
  query: Query;
  referencedComponentsById: Dict<ReferencedComponent>;
  referencedComponentsByKey: Dict<ReferencedComponent>;
  referencedLanguages: Dict<ReferencedLanguage>;
  referencedRules: Dict<ReferencedRule>;
  referencedUsers: Dict<UserBase>;
  selected?: string;
  selectedFlowIndex?: number;
  selectedLocationIndex?: number;
}

const DEFAULT_QUERY = { resolved: 'false' };
const MAX_INITAL_FETCH = 1000;
const BRANCH_STATUS_REFRESH_INTERVAL = 1000;

export class App extends React.PureComponent<Props, State> {
  mounted = false;
  bulkButtonRef: React.RefObject<HTMLButtonElement>;

  constructor(props: Props) {
    super(props);
    const query = parseQuery(props.location.query);
    this.bulkButtonRef = React.createRef();
    this.state = {
      bulkChangeModal: false,
      checked: [],
      facets: {},
      issues: [],
      loading: true,
      loadingFacets: {},
      loadingRule: false,
      loadingMore: false,
      locationsNavigator: false,
      myIssues: areMyIssuesSelected(props.location.query),
      openFacets: {
        owaspTop10: shouldOpenStandardsChildFacet({}, query, SecurityStandard.OWASP_TOP10),
        'owaspTop10-2021': shouldOpenStandardsChildFacet(
          {},
          query,
          SecurityStandard.OWASP_TOP10_2021
        ),
        sansTop25: shouldOpenStandardsChildFacet({}, query, SecurityStandard.SANS_TOP25),
        severities: true,
        sonarsourceSecurity: shouldOpenSonarSourceSecurityFacet({}, query),
        standards: shouldOpenStandardsFacet({}, query),
        types: true,
      },
      query,
      referencedComponentsById: {},
      referencedComponentsByKey: {},
      referencedLanguages: {},
      referencedRules: {},
      referencedUsers: {},
      selected: getOpen(props.location.query),
    };
    this.refreshBranchStatus = debounce(this.refreshBranchStatus, BRANCH_STATUS_REFRESH_INTERVAL);
  }

  static getDerivedStateFromProps(props: Props, state: State) {
    const {
      location: { query },
    } = props;

    return {
      myIssues: areMyIssuesSelected(query),
      query: parseQuery(query),
      openIssue: getOpenIssue(props, state.issues),
    };
  }

  componentDidMount() {
    this.mounted = true;

    if (this.state.myIssues && !this.props.currentUser.isLoggedIn) {
      handleRequiredAuthentication();
      return;
    }

    addWhitePageClass();
    addSideBarClass();
    this.attachShortcuts();
    this.fetchFirstIssues();
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    const { query } = this.props.location;
    const { query: prevQuery } = prevProps.location;
    const { openIssue } = this.state;

    if (
      prevProps.component !== this.props.component ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
      !areQueriesEqual(prevQuery, query) ||
      areMyIssuesSelected(prevQuery) !== areMyIssuesSelected(query)
    ) {
      this.fetchFirstIssues();
      this.setState({ checkAll: false });
    } else if (openIssue && openIssue.key !== this.state.selected) {
      this.setState({
        locationsNavigator: true,
        selected: openIssue.key,
        selectedFlowIndex: 0,
        selectedLocationIndex: undefined,
      });
    }
    if (this.state.openIssue && this.state.openIssue.key !== prevState.openIssue?.key) {
      this.loadRule();
    }
  }

  componentWillUnmount() {
    this.detachShortcuts();
    this.mounted = false;
    removeWhitePageClass();
    removeSideBarClass();
  }

  attachShortcuts() {
    document.addEventListener('keydown', this.handleKeyDown);
    document.addEventListener('keyup', this.handleKeyUp);
  }

  detachShortcuts() {
    document.removeEventListener('keydown', this.handleKeyDown);
    document.removeEventListener('keyup', this.handleKeyUp);
  }

  handleKeyDown = (event: KeyboardEvent) => {
    // Ignore if modal is open
    if (this.state.bulkChangeModal) {
      return;
    }

    if (isInput(event) || isShortcut(event)) {
      return true;
    }

    if (event.key === KeyboardKeys.Alt) {
      event.preventDefault();
      this.setState(actions.enableLocationsNavigator);
      return;
    }

    switch (event.key) {
      case KeyboardKeys.DownArrow: {
        event.preventDefault();
        if (event.altKey) {
          this.selectNextLocation();
        } else {
          this.selectNextIssue();
        }
        break;
      }
      case KeyboardKeys.UpArrow: {
        event.preventDefault();
        if (event.altKey) {
          this.selectPreviousLocation();
        } else {
          this.selectPreviousIssue();
        }
        break;
      }
      case KeyboardKeys.LeftArrow: {
        event.preventDefault();
        if (event.altKey) {
          this.selectPreviousFlow();
        } else {
          this.closeIssue();
        }
        break;
      }
      case KeyboardKeys.RightArrow: {
        event.preventDefault();
        if (event.altKey) {
          this.selectNextFlow();
        } else {
          this.openSelectedIssue();
        }
        break;
      }
    }
  };

  handleKeyUp = (event: KeyboardEvent) => {
    if (event.key === KeyboardKeys.Alt) {
      this.setState(actions.disableLocationsNavigator);
    }
  };

  getSelectedIndex() {
    const { issues = [], selected } = this.state;
    const index = issues.findIndex((issue) => issue.key === selected);
    return index !== -1 ? index : undefined;
  }

  selectNextIssue = () => {
    const { issues } = this.state;
    const selectedIndex = this.getSelectedIndex();
    if (selectedIndex !== undefined && selectedIndex < issues.length - 1) {
      if (this.state.openIssue) {
        this.openIssue(issues[selectedIndex + 1].key);
      } else {
        this.setState({
          selected: issues[selectedIndex + 1].key,
          selectedFlowIndex: undefined,
          selectedLocationIndex: undefined,
        });
      }
    }
  };

  async loadRule() {
    const { openIssue } = this.state;
    if (openIssue === undefined) {
      return;
    }
    this.setState({ loadingRule: true });
    const openRuleDetails = await getRuleDetails({ key: openIssue.rule })
      .then((response) => response.rule)
      .catch(() => undefined);
    if (this.mounted) {
      this.setState({ loadingRule: false, openRuleDetails });
    }
  }

  selectPreviousIssue = () => {
    const { issues } = this.state;
    const selectedIndex = this.getSelectedIndex();
    if (selectedIndex !== undefined && selectedIndex > 0) {
      if (this.state.openIssue) {
        this.openIssue(issues[selectedIndex - 1].key);
      } else {
        this.setState({
          selected: issues[selectedIndex - 1].key,
          selectedFlowIndex: undefined,
          selectedLocationIndex: undefined,
        });
      }
    }
  };

  openIssue = (issueKey: string) => {
    const path = {
      pathname: this.props.location.pathname,
      query: {
        ...serializeQuery(this.state.query),
        ...getBranchLikeQuery(this.props.branchLike),
        id: this.props.component && this.props.component.key,
        myIssues: this.state.myIssues ? 'true' : undefined,
        open: issueKey,
      },
    };
    if (this.state.openIssue) {
      if (path.query.open && path.query.open === this.state.openIssue.key) {
        this.setState({
          locationsNavigator: false,
          selectedLocationIndex: -1,
        });
      } else {
        this.props.router.replace(path);
      }
    } else {
      this.props.router.push(path);
    }
  };

  closeIssue = () => {
    if (this.state.query) {
      this.props.router.push({
        pathname: this.props.location.pathname,
        query: {
          ...serializeQuery(this.state.query),
          ...getBranchLikeQuery(this.props.branchLike),
          id: this.props.component && this.props.component.key,
          myIssues: this.state.myIssues ? 'true' : undefined,
          open: undefined,
        },
      });
    }
  };

  openSelectedIssue = () => {
    const { selected } = this.state;
    if (selected) {
      this.openIssue(selected);
    }
  };

  createdAfterIncludesTime = () => Boolean(this.props.location.query.createdAfter?.includes('T'));

  fetchIssuesHelper = (query: RawQuery) => {
    return searchIssues({
      ...query,
      additionalFields: '_all',
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    }).then((response) => {
      const parsedIssues = response.issues.map((issue) =>
        parseIssueFromResponse(issue, response.components, response.users, response.rules)
      );
      return { ...response, issues: parsedIssues } as FetchIssuesPromise;
    });
  };

  fetchIssues = (additional: RawQuery, requestFacets = false): Promise<FetchIssuesPromise> => {
    const { component } = this.props;
    const { myIssues, openFacets, query } = this.state;

    const facets = requestFacets
      ? Object.keys(openFacets)
          .filter((facet) => facet !== STANDARDS && openFacets[facet])
          .join(',')
      : undefined;

    const parameters: Dict<string | undefined> = {
      ...getBranchLikeQuery(this.props.branchLike),
      componentKeys: component && component.key,
      s: 'FILE_LINE',
      ...serializeQuery(query),
      ps: '100',
      facets,
      ...additional,
    };

    if (query.createdAfter !== undefined && this.createdAfterIncludesTime()) {
      parameters.createdAfter = serializeDate(query.createdAfter);
    }

    // only sorting by CREATION_DATE is allowed, so let's sort DESC
    if (query.sort) {
      Object.assign(parameters, { asc: 'false' });
    }

    if (myIssues) {
      Object.assign(parameters, { assignees: '__me__' });
    }

    return this.fetchIssuesHelper(parameters);
  };

  fetchFirstIssues() {
    const prevQuery = this.props.location.query;
    const openIssueKey = getOpen(this.props.location.query);
    let fetchPromise;

    this.setState({ checked: [], loading: true });
    if (openIssueKey !== undefined) {
      fetchPromise = this.fetchIssuesUntil(1, (pageIssues: Issue[], paging: Paging) => {
        if (
          paging.total <= paging.pageIndex * paging.pageSize ||
          paging.pageIndex * paging.pageSize >= MAX_INITAL_FETCH
        ) {
          return true;
        }
        return pageIssues.some((issue) => issue.key === openIssueKey);
      });
    } else {
      fetchPromise = this.fetchIssues({}, true);
    }

    return fetchPromise.then(
      ({ effortTotal, facets, issues, paging, ...other }) => {
        if (this.mounted && areQueriesEqual(prevQuery, this.props.location.query)) {
          const openIssue = getOpenIssue(this.props, issues);
          let selected: string | undefined = undefined;
          if (issues.length > 0) {
            selected = openIssue ? openIssue.key : issues[0].key;
          }
          this.setState((state) => ({
            cannotShowOpenIssue: Boolean(openIssueKey && !openIssue),
            effortTotal,
            facets: { ...state.facets, ...parseFacets(facets) },
            loading: false,
            locationsNavigator: true,
            issues,
            openIssue,
            paging,
            referencedComponentsById: keyBy(other.components, 'uuid'),
            referencedComponentsByKey: keyBy(other.components, 'key'),
            referencedLanguages: keyBy(other.languages, 'key'),
            referencedRules: keyBy(other.rules, 'key'),
            referencedUsers: keyBy(other.users, 'login'),
            selected,
            selectedFlowIndex: 0,
            selectedLocationIndex: undefined,
          }));
        }
        return issues;
      },
      () => {
        if (this.mounted && areQueriesEqual(prevQuery, this.props.location.query)) {
          this.setState({ loading: false });
        }
        return [];
      }
    );
  }

  fetchIssuesPage = (p: number) => {
    return this.fetchIssues({ p });
  };

  fetchIssuesUntil = (
    page: number,
    done: (pageIssues: Issue[], paging: Paging) => boolean
  ): Promise<FetchIssuesPromise> => {
    const recursiveFetch = (p: number, prevIssues: Issue[]): Promise<FetchIssuesPromise> => {
      return this.fetchIssuesPage(p).then(({ issues: pageIssues, paging, ...other }) => {
        const issues = [...prevIssues, ...pageIssues];
        return done(pageIssues, paging)
          ? { issues, paging, ...other }
          : recursiveFetch(p + 1, issues);
      });
    };

    return recursiveFetch(page, []);
  };

  fetchMoreIssues = () => {
    const { paging } = this.state;

    if (!paging) {
      return Promise.reject();
    }

    const p = paging.pageIndex + 1;

    this.setState({ checkAll: false, loadingMore: true });
    return this.fetchIssuesPage(p).then(
      (response) => {
        if (this.mounted) {
          this.setState((state) => ({
            loadingMore: false,
            issues: [...state.issues, ...response.issues],
            paging: response.paging,
          }));
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loadingMore: false });
        }
      }
    );
  };

  fetchFacet = (facet: string) => {
    return this.fetchIssues({ ps: 1, facets: facet }, false).then(
      ({ facets, ...other }) => {
        if (this.mounted) {
          this.setState((state) => ({
            facets: { ...state.facets, ...parseFacets(facets) },
            loadingFacets: omit(state.loadingFacets, facet),
            referencedComponentsById: {
              ...state.referencedComponentsById,
              ...keyBy(other.components, 'uuid'),
            },
            referencedComponentsByKey: {
              ...state.referencedComponentsByKey,
              ...keyBy(other.components, 'key'),
            },
            referencedLanguages: {
              ...state.referencedLanguages,
              ...keyBy(other.languages, 'key'),
            },
            referencedRules: { ...state.referencedRules, ...keyBy(other.rules, 'key') },
            referencedUsers: { ...state.referencedUsers, ...keyBy(other.users, 'login') },
          }));
        }
      },
      () => {
        /* Do nothing */
      }
    );
  };

  isFiltered = () => {
    const serialized = serializeQuery(this.state.query);
    return !areQueriesEqual(serialized, DEFAULT_QUERY);
  };

  getCheckedIssues = () => {
    const issues = this.state.checked
      .map((checked) => this.state.issues.find((issue) => issue.key === checked))
      .filter((issue): issue is Issue => issue !== undefined);
    const paging = { pageIndex: 1, pageSize: issues.length, total: issues.length };
    return Promise.resolve({ issues, paging });
  };

  getButtonLabel = (checked: string[], checkAll = false, paging?: Paging) => {
    if (checked.length === 0) {
      return translate('bulk_change');
    }

    let count;
    if (checkAll && paging) {
      count = paging.total > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : paging.total;
    } else {
      count = Math.min(checked.length, MAX_PAGE_SIZE);
    }

    return translateWithParameters('issues.bulk_change_X_issues', count);
  };

  handleFilterChange = (changes: Partial<Query>) => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...serializeQuery({ ...this.state.query, ...changes }),
        ...getBranchLikeQuery(this.props.branchLike),
        id: this.props.component && this.props.component.key,
        myIssues: this.state.myIssues ? 'true' : undefined,
      },
    });
    this.setState(({ openFacets }) => ({
      openFacets: {
        ...openFacets,
        sonarsourceSecurity: shouldOpenSonarSourceSecurityFacet(openFacets, changes),
        standards: shouldOpenStandardsFacet(openFacets, changes),
      },
    }));
  };

  handleMyIssuesChange = (myIssues: boolean) => {
    this.closeFacet('assignees');
    if (!this.props.component) {
      saveMyIssues(myIssues);
    }
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...serializeQuery({ ...this.state.query, assigned: true, assignees: [] }),
        ...getBranchLikeQuery(this.props.branchLike),
        id: this.props.component && this.props.component.key,
        myIssues: myIssues ? 'true' : undefined,
      },
    });
  };

  loadSearchResultCount = (property: string, changes: Partial<Query>) => {
    const { component } = this.props;
    const { myIssues, query } = this.state;

    const parameters = {
      ...getBranchLikeQuery(this.props.branchLike),
      componentKeys: component && component.key,
      facets: property,
      s: 'FILE_LINE',
      ...serializeQuery({ ...query, ...changes }),
      ps: 1,
    };

    if (myIssues) {
      Object.assign(parameters, { assignees: '__me__' });
    }

    return this.fetchIssuesHelper(parameters).then(({ facets }) => parseFacets(facets)[property]);
  };

  closeFacet = (property: string) => {
    this.setState((state) => ({
      openFacets: { ...state.openFacets, [property]: false },
    }));
  };

  handleFacetToggle = (property: string) => {
    this.setState((state) => {
      const willOpenProperty = !state.openFacets[property];
      const newState = {
        loadingFacets: state.loadingFacets,
        openFacets: { ...state.openFacets, [property]: willOpenProperty },
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

      // No need to load facets data for standard facet
      if (property !== STANDARDS && !state.facets[property]) {
        newState.loadingFacets[property] = true;
        this.fetchFacet(property);
      }

      return newState;
    });
  };

  handleReset = () => {
    this.props.router.push({
      pathname: this.props.location.pathname,
      query: {
        ...DEFAULT_QUERY,
        ...getBranchLikeQuery(this.props.branchLike),
        id: this.props.component && this.props.component.key,
        myIssues: this.state.myIssues ? 'true' : undefined,
      },
    });
  };

  handlePopupToggle = (issue: string, popupName: string, open: boolean | undefined = undefined) => {
    this.setState((state: State) => {
      const { openPopup } = state;
      const samePopup = openPopup && openPopup.name === popupName && openPopup.issue === issue;

      if (open !== false && !samePopup) {
        return { ...state, openPopup: { issue, name: popupName } };
      } else if (open !== true && samePopup) {
        return { ...state, openPopup: undefined };
      }

      return state;
    });
  };

  handleIssueCheck = (issue: string) => {
    this.setState((state) => ({
      checkAll: false,
      checked: state.checked.includes(issue)
        ? without(state.checked, issue)
        : [...state.checked, issue],
    }));
  };

  handleIssueChange = (issue: Issue) => {
    this.refreshBranchStatus();
    this.setState((state) => ({
      issues: state.issues.map((candidate) => (candidate.key === issue.key ? issue : candidate)),
    }));
  };

  handleOpenBulkChange = () => {
    this.setState({ bulkChangeModal: true });
  };

  handleCloseBulkChange = () => {
    this.setState({ bulkChangeModal: false }, () => {
      if (this.bulkButtonRef.current) {
        this.bulkButtonRef.current.focus();
      }
    });
  };

  handleBulkChangeDone = () => {
    this.setState({ checkAll: false });
    this.refreshBranchStatus();
    this.fetchFirstIssues();
    this.handleCloseBulkChange();
  };

  selectLocation = (index: number) => {
    const { selectedLocationIndex } = this.state;
    if (index === selectedLocationIndex) {
      this.setState({ selectedLocationIndex: undefined }, () => {
        this.setState({ selectedLocationIndex: index });
      });
    } else {
      this.setState(({ openIssue }) => {
        if (openIssue) {
          return { locationsNavigator: true, selectedLocationIndex: index };
        }
        return null;
      });
    }
  };

  selectNextLocation = () => {
    this.setState(actions.selectNextLocation);
  };

  selectPreviousLocation = () => {
    this.setState(actions.selectPreviousLocation);
  };

  handleCheckAll = (checked: boolean) => {
    if (checked) {
      this.setState((state) => ({
        checkAll: true,
        checked: state.issues.map((issue) => issue.key),
      }));
    } else {
      this.setState({ checkAll: false, checked: [] });
    }
  };

  selectFlow = (index?: number) => {
    this.setState(actions.selectFlow(index));
  };

  selectNextFlow = () => {
    this.setState(actions.selectNextFlow);
  };

  selectPreviousFlow = () => {
    this.setState(actions.selectPreviousFlow);
  };

  refreshBranchStatus = () => {
    const { branchLike, component } = this.props;
    if (branchLike && component && isPullRequest(branchLike)) {
      this.props.fetchBranchStatus(branchLike, component.key);
    }
  };

  renderBulkChange() {
    const { component, currentUser } = this.props;
    const { checkAll, bulkChangeModal, checked, issues, paging } = this.state;

    const isAllChecked = checked.length > 0 && issues.length === checked.length;
    const thirdState = checked.length > 0 && !isAllChecked;
    const isChecked = isAllChecked || thirdState;

    if (!currentUser.isLoggedIn) {
      return null;
    }

    return (
      <div className="pull-left">
        <Checkbox
          checked={isChecked}
          className="spacer-right text-middle"
          disabled={issues.length === 0}
          id="issues-selection"
          onCheck={this.handleCheckAll}
          thirdState={thirdState}
          title={translate('issues.select_all_issues')}
        />
        <Button
          innerRef={this.bulkButtonRef}
          disabled={checked.length === 0}
          id="issues-bulk-change"
          onClick={this.handleOpenBulkChange}
        >
          {this.getButtonLabel(checked, checkAll, paging)}
        </Button>

        {bulkChangeModal && (
          <BulkChangeModal
            component={component}
            currentUser={currentUser}
            fetchIssues={checkAll ? this.fetchIssues : this.getCheckedIssues}
            onClose={this.handleCloseBulkChange}
            onDone={this.handleBulkChangeDone}
          />
        )}
      </div>
    );
  }

  renderFacets() {
    const { component, currentUser, branchLike } = this.props;
    const { query } = this.state;

    return (
      <div className="layout-page-filters">
        {currentUser.isLoggedIn && (
          <div className="display-flex-justify-center big-spacer-bottom">
            <ButtonToggle
              options={[
                { value: true, label: translate('issues.my_issues') },
                { value: false, label: translate('all') },
              ]}
              value={this.state.myIssues}
              onCheck={this.handleMyIssuesChange}
            />
          </div>
        )}
        <FiltersHeader displayReset={this.isFiltered()} onReset={this.handleReset} />
        <Sidebar
          branchLike={branchLike}
          component={component}
          createdAfterIncludesTime={this.createdAfterIncludesTime()}
          facets={this.state.facets}
          loadSearchResultCount={this.loadSearchResultCount}
          loadingFacets={this.state.loadingFacets}
          myIssues={this.state.myIssues}
          onFacetToggle={this.handleFacetToggle}
          onFilterChange={this.handleFilterChange}
          openFacets={this.state.openFacets}
          query={query}
          referencedComponentsById={this.state.referencedComponentsById}
          referencedComponentsByKey={this.state.referencedComponentsByKey}
          referencedLanguages={this.state.referencedLanguages}
          referencedRules={this.state.referencedRules}
          referencedUsers={this.state.referencedUsers}
        />
      </div>
    );
  }

  renderConciseIssuesList() {
    const { issues, loadingMore, paging, query } = this.state;

    return (
      <div className="layout-page-filters">
        <ConciseIssuesListHeader
          displayBackButton={query.issues.length !== 1}
          loading={this.state.loading}
          onBackClick={this.closeIssue}
        />
        <ConciseIssuesList
          issues={issues}
          onFlowSelect={this.selectFlow}
          onIssueSelect={this.openIssue}
          onLocationSelect={this.selectLocation}
          selected={this.state.selected}
          selectedFlowIndex={this.state.selectedFlowIndex}
          selectedLocationIndex={this.state.selectedLocationIndex}
        />
        {paging && paging.total > 0 && (
          <ListFooter
            count={issues.length}
            loadMore={this.fetchMoreIssues}
            loading={loadingMore}
            total={paging.total}
          />
        )}
      </div>
    );
  }

  renderSide(openIssue: Issue | undefined) {
    const { canBrowseAllChildProjects, qualifier = ComponentQualifier.Project } =
      this.props.component || {};
    return (
      <ScreenPositionHelper className="layout-page-side-outer">
        {({ top }) => (
          <section
            aria-label={openIssue ? translate('list_of_issues') : translate('filters')}
            className="layout-page-side"
            style={{ top }}
          >
            <div className="layout-page-side-inner">
              <A11ySkipTarget
                anchor="issues_sidebar"
                label={
                  openIssue ? translate('issues.skip_to_list') : translate('issues.skip_to_filters')
                }
                weight={10}
              />
              {!canBrowseAllChildProjects && isPortfolioLike(qualifier) && (
                <div
                  className={classNames('not-all-issue-warning', {
                    'open-issue-list': openIssue,
                  })}
                >
                  <Alert className={classNames('it__portfolio_warning')} variant="warning">
                    <AlertContent>
                      {translate('issues.not_all_issue_show')}
                      <HelpTooltip
                        className="spacer-left"
                        ariaLabel={translate('issues.not_all_issue_show_why')}
                        overlay={translate('issues.not_all_issue_show_why')}
                      />
                    </AlertContent>
                  </Alert>
                </div>
              )}

              {openIssue ? this.renderConciseIssuesList() : this.renderFacets()}
            </div>
          </section>
        )}
      </ScreenPositionHelper>
    );
  }

  renderList() {
    const { branchLike, component, currentUser } = this.props;
    const { issues, loading, loadingMore, openIssue, paging } = this.state;
    const selectedIndex = this.getSelectedIndex();
    const selectedIssue = selectedIndex !== undefined ? issues[selectedIndex] : undefined;

    if (!paging || openIssue) {
      return null;
    }

    let noIssuesMessage = null;
    if (paging.total === 0 && !loading) {
      if (this.isFiltered()) {
        noIssuesMessage = <EmptySearch />;
      } else if (this.state.myIssues) {
        noIssuesMessage = <NoMyIssues />;
      } else {
        noIssuesMessage = <NoIssues />;
      }
    }

    return (
      <div>
        <h2 className="a11y-hidden">{translate('list_of_issues')}</h2>
        {paging.total > 0 && (
          <IssuesList
            branchLike={branchLike}
            checked={this.state.checked}
            component={component}
            issues={issues}
            onFilterChange={this.handleFilterChange}
            onIssueChange={this.handleIssueChange}
            onIssueCheck={currentUser.isLoggedIn ? this.handleIssueCheck : undefined}
            onIssueClick={this.openIssue}
            onPopupToggle={this.handlePopupToggle}
            openPopup={this.state.openPopup}
            selectedIssue={selectedIssue}
          />
        )}

        {paging.total > 0 && (
          <ListFooter
            count={issues.length}
            loadMore={this.fetchMoreIssues}
            loading={loadingMore}
            total={paging.total}
          />
        )}

        {noIssuesMessage}
      </div>
    );
  }

  renderHeader({
    openIssue,
    paging,
    selectedIndex,
  }: {
    openIssue: Issue | undefined;
    paging: Paging | undefined;
    selectedIndex: number | undefined;
  }) {
    return openIssue ? (
      <A11ySkipTarget anchor="issues_main" />
    ) : (
      <div className="layout-page-header-panel layout-page-main-header issues-main-header">
        <div className="layout-page-header-panel-inner layout-page-main-header-inner">
          <div className="layout-page-main-inner">
            <A11ySkipTarget anchor="issues_main" />

            {this.renderBulkChange()}
            <PageActions
              canSetHome={!this.props.component}
              effortTotal={this.state.effortTotal}
              paging={paging}
              selectedIndex={selectedIndex}
            />
          </div>
        </div>
      </div>
    );
  }

  renderPage() {
    const {
      cannotShowOpenIssue,
      openRuleDetails,
      checkAll,
      issues,
      loading,
      openIssue,
      paging,
      loadingRule,
    } = this.state;
    return (
      <div className="layout-page-main-inner">
        <DeferredSpinner loading={loadingRule}>
          {openIssue && openRuleDetails ? (
            <>
              <IssueHeader
                issue={openIssue}
                ruleDetails={openRuleDetails}
                branchLike={fillBranchLike(openIssue.branch, openIssue.pullRequest)}
                onIssueChange={this.handleIssueChange}
              />
              <RuleTabViewer
                ruleDetails={openRuleDetails}
                extendedDescription={openRuleDetails.htmlNote}
                ruleDescriptionContextKey={openIssue.ruleDescriptionContextKey}
                codeTabContent={
                  <IssuesSourceViewer
                    branchLike={fillBranchLike(openIssue.branch, openIssue.pullRequest)}
                    issues={issues}
                    locationsNavigator={this.state.locationsNavigator}
                    onIssueSelect={this.openIssue}
                    onLocationSelect={this.selectLocation}
                    openIssue={openIssue}
                    selectedFlowIndex={this.state.selectedFlowIndex}
                    selectedLocationIndex={this.state.selectedLocationIndex}
                  />
                }
                scrollInTab={true}
              />
            </>
          ) : (
            <DeferredSpinner loading={loading} ariaLabel={translate('issues.loading_issues')}>
              {checkAll && paging && paging.total > MAX_PAGE_SIZE && (
                <Alert className="big-spacer-bottom" variant="warning">
                  <FormattedMessage
                    defaultMessage={translate('issue_bulk_change.max_issues_reached')}
                    id="issue_bulk_change.max_issues_reached"
                    values={{ max: <strong>{MAX_PAGE_SIZE}</strong> }}
                  />
                </Alert>
              )}
              {cannotShowOpenIssue && (!paging || paging.total > 0) && (
                <Alert className="big-spacer-bottom" variant="warning">
                  {translateWithParameters(
                    'issues.cannot_open_issue_max_initial_X_fetched',
                    MAX_INITAL_FETCH
                  )}
                </Alert>
              )}
              {this.renderList()}
            </DeferredSpinner>
          )}
        </DeferredSpinner>
      </div>
    );
  }

  render() {
    const { component } = this.props;
    const { openIssue, paging } = this.state;
    const selectedIndex = this.getSelectedIndex();
    return (
      <div
        className={classNames('layout-page issues', { 'project-level': component !== undefined })}
        id="issues-page"
      >
        <Suggestions suggestions="issues" />
        <Helmet defer={false} title={openIssue ? openIssue.message : translate('issues.page')} />

        <h1 className="a11y-hidden">{translate('issues.page')}</h1>

        {this.renderSide(openIssue)}

        <div role="main" className={classNames('layout-page-main', { 'open-issue': !!openIssue })}>
          {this.renderHeader({ openIssue, paging, selectedIndex })}

          {this.renderPage()}
        </div>
      </div>
    );
  }
}

const AlertContent = styled.div`
  display: flex;
  align-items: center;
`;

export default withIndexationGuard(
  withRouter(withCurrentUserContext(withBranchStatusActions(withComponentContext(App)))),
  PageContext.Issues
);
