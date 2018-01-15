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
import classNames from 'classnames';
import { intersection, uniqBy } from 'lodash';
import SourceViewerHeader from './SourceViewerHeader';
import SourceViewerCode from './SourceViewerCode';
import CoveragePopupView from './popups/coverage-popup';
import DuplicationPopupView from './popups/duplication-popup';
import LineActionsPopupView from './popups/line-actions-popup';
import SCMPopupView from './popups/scm-popup';
import MeasuresOverlay from './views/measures-overlay';
import loadIssues from './helpers/loadIssues';
import getCoverageStatus from './helpers/getCoverageStatus';
import {
  issuesByLine,
  locationsByLine,
  duplicationsByLine,
  symbolsByLine
} from './helpers/indexing';
/*:: import type { LinearIssueLocation } from './helpers/indexing'; */
import {
  getComponentForSourceViewer,
  getComponentData,
  getSources,
  getDuplications,
  getTests
} from '../../api/components';
import { parseDate } from '../../helpers/dates';
import { translate } from '../../helpers/l10n';
import { scrollToElement } from '../../helpers/scrolling';
/*:: import type { SourceLine } from './types'; */
/*:: import type { Issue, FlowLocation } from '../issue/types'; */
import './styles.css';

// TODO react-virtualized

/*::
type Props = {
  aroundLine?: number,
  branch?: string,
  component: string,
  displayAllIssues: boolean,
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayLocationMarkers?: boolean;
  highlightedLine?: number,
  highlightedLocations?: Array<FlowLocation>,
  highlightedLocationMessage?: { index: number, text: string },
  loadComponent: (component: string, branch?: string) => Promise<*>,
  loadIssues: (component: string, from: number, to: number, branch?: string) => Promise<*>,
  loadSources: (component: string, from: number, to: number, branch?: string) => Promise<*>,
  onLoaded?: (component: Object, sources: Array<*>, issues: Array<*>) => void,
  onLocationSelect?: number => void,
  onIssueChange?: Issue => void,
  onIssueSelect?: string => void,
  onIssueUnselect?: () => void,
  onReceiveComponent: ({ canMarkAsFavorite: boolean, fav: boolean, key: string }) => void,
  scroll?: HTMLElement => void,
  selectedIssue?: string
};
*/

/*::
type State = {
  component?: Object,
  displayDuplications: boolean,
  duplications?: Array<{
    blocks: Array<{
      _ref: string,
      from: number,
      size: number
    }>
  }>,
  duplicationsByLine: { [number]: Array<number> },
  duplicatedFiles?: Array<{ key: string }>,
  hasSourcesAfter: boolean,
  highlightedLine: number | null,
  highlightedSymbols: Array<string>,
  issues?: Array<Issue>,
  issuesByLine: { [number]: Array<Issue> },
  issueLocationsByLine: { [number]: Array<LinearIssueLocation> },
  loading: boolean,
  loadingSourcesAfter: boolean,
  loadingSourcesBefore: boolean,
  notAccessible: boolean,
  notExist: boolean,
  openIssuesByLine: { [number]: boolean },
  openPopup: ?{
    issue: string,
    name: string
  },
  selectedIssue?: string,
  sources?: Array<SourceLine>,
  sourceRemoved: boolean,
  symbolsByLine: { [number]: Array<string> }
};
*/

const LINES = 500;

function loadComponent(key /*: string */, branch /*: string | void */) /*: Promise<*> */ {
  return Promise.all([
    getComponentForSourceViewer(key, branch),
    getComponentData(key, branch)
  ]).then(([component, data]) => ({
    ...component,
    leakPeriodDate: data.leakPeriodDate && parseDate(data.leakPeriodDate)
  }));
}

function loadSources(
  key /*: string */,
  from /*: ?number */,
  to /*: ?number */,
  branch /*: string | void */
) /*: Promise<Array<*>> */ {
  return getSources(key, from, to, branch);
}

export default class SourceViewerBase extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: node: HTMLElement; */
  /*:: props: Props; */
  /*:: state: State; */

  static defaultProps = {
    displayAllIssues: false,
    displayIssueLocationsCount: true,
    displayIssueLocationsLink: true,
    displayLocationMarkers: true,
    loadComponent,
    loadIssues,
    loadSources
  };

  constructor(props /*: Props */) {
    super(props);
    this.state = {
      displayDuplications: false,
      duplicationsByLine: {},
      hasSourcesAfter: false,
      highlightedLine: props.highlightedLine || null,
      highlightedSymbols: [],
      issuesByLine: {},
      issueLocationsByLine: {},
      issueSecondaryLocationsByIssueByLine: {},
      issueSecondaryLocationMessagesByIssueByLine: {},
      loading: true,
      loadingSourcesAfter: false,
      loadingSourcesBefore: false,
      notAccessible: false,
      notExist: false,
      openIssuesByLine: {},
      openPopup: null,
      selectedIssue: props.selectedIssue,
      selectedIssueLocation: null,
      sourceRemoved: false,
      symbolsByLine: {}
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.onIssueSelect != null && nextProps.selectedIssue !== this.props.selectedIssue) {
      this.setState({ selectedIssue: nextProps.selectedIssue });
    }
  }

  componentDidUpdate(prevProps /*: Props */) {
    if (prevProps.component !== this.props.component || prevProps.branch !== this.props.branch) {
      this.fetchComponent();
    } else if (
      this.props.aroundLine != null &&
      prevProps.aroundLine !== this.props.aroundLine &&
      this.isLineOutsideOfRange(this.props.aroundLine)
    ) {
      this.fetchSources();
    } else {
      const { selectedIssue } = this.props;
      const { issues } = this.state;
      if (
        selectedIssue != null &&
        issues != null &&
        issues.find(issue => issue.key === selectedIssue) == null
      ) {
        this.reloadIssues();
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  scrollToLine(line /*: number */) {
    const lineElement = this.node.querySelector(
      `.source-line-code[data-line-number="${line}"] .source-line-issue-locations`
    );
    if (lineElement) {
      scrollToElement(lineElement, { topOffset: 125, bottomOffset: 75 });
    }
  }

  computeCoverageStatus(lines /*: Array<SourceLine> */) /*: Array<SourceLine> */ {
    return lines.map(line => ({ ...line, coverageStatus: getCoverageStatus(line) }));
  }

  isLineOutsideOfRange(lineNumber /*: number */) {
    const { sources } = this.state;
    if (sources != null && sources.length > 0) {
      const firstLine = sources[0];
      const lastList = sources[sources.length - 1];
      return lineNumber < firstLine.line || lineNumber > lastList.line;
    } else {
      return true;
    }
  }

  fetchComponent() {
    this.setState({ loading: true });
    const loadIssues = (component, sources) => {
      this.props.loadIssues(this.props.component, 1, LINES, this.props.branch).then(issues => {
        if (this.mounted) {
          const finalSources = sources.slice(0, LINES);
          this.setState(
            {
              component,
              issues,
              issuesByLine: issuesByLine(issues),
              issueLocationsByLine: locationsByLine(issues),
              loading: false,
              notAccessible: false,
              notExist: false,
              hasSourcesAfter: sources.length > LINES,
              sources: this.computeCoverageStatus(finalSources),
              sourceRemoved: false,
              symbolsByLine: symbolsByLine(sources.slice(0, LINES))
            },
            () => {
              if (this.props.onLoaded) {
                this.props.onLoaded(component, finalSources, issues);
              }
            }
          );
        }
      });
    };

    const onFailLoadComponent = ({ response }) => {
      // TODO handle other statuses
      if (this.mounted) {
        if (response.status === 403) {
          this.setState({ loading: false, notAccessible: true });
        } else if (response.status === 404) {
          this.setState({ loading: false, notExist: true });
        }
      }
    };

    const onFailLoadSources = (response, component) => {
      // TODO handle other statuses
      if (this.mounted) {
        if (response.status === 403) {
          this.setState({ component, loading: false, notAccessible: true });
        } else if (response.status === 404) {
          this.setState({ component, loading: false, sourceRemoved: true });
        }
      }
    };

    const onResolve = component => {
      this.props.onReceiveComponent(component);
      const sourcesRequest =
        component.q === 'FIL' || component.q === 'UTS' ? this.loadSources() : Promise.resolve([]);
      sourcesRequest.then(
        sources => loadIssues(component, sources),
        response => onFailLoadSources(response, component)
      );
    };

    this.props
      .loadComponent(this.props.component, this.props.branch)
      .then(onResolve, onFailLoadComponent);
  }

  fetchSources() {
    this.loadSources().then(sources => {
      if (this.mounted) {
        const finalSources = sources.slice(0, LINES);
        this.setState(
          {
            sources: sources.slice(0, LINES),
            hasSourcesAfter: sources.length > LINES
          },
          () => {
            if (this.props.onLoaded) {
              // $FlowFixMe
              this.props.onLoaded(this.state.component, finalSources, this.state.issues);
            }
          }
        );
      }
    });
  }

  reloadIssues() {
    if (!this.state.sources) {
      return;
    }
    const firstSourceLine = this.state.sources[0];
    const lastSourceLine = this.state.sources[this.state.sources.length - 1];
    this.props
      .loadIssues(
        this.props.component,
        firstSourceLine && firstSourceLine.line,
        lastSourceLine && lastSourceLine.line
      )
      .then(issues => {
        if (this.mounted) {
          this.setState({
            issues,
            issuesByLine: issuesByLine(issues),
            issueLocationsByLine: locationsByLine(issues)
          });
        }
      });
  }

  loadSources() {
    return new Promise((resolve, reject) => {
      const onFailLoadSources = ({ response }) => {
        // TODO handle other statuses
        if (this.mounted) {
          if ([403, 404].includes(response.status)) {
            reject(response);
          } else {
            resolve([]);
          }
        }
      };

      const from = this.props.aroundLine ? Math.max(1, this.props.aroundLine - LINES / 2 + 1) : 1;

      let to = this.props.aroundLine ? this.props.aroundLine + LINES / 2 + 1 : LINES + 1;
      // make sure we try to download `LINES` lines
      if (from === 1 && to < LINES) {
        to = LINES;
      }
      // request one additional line to define `hasSourcesAfter`
      to++;

      return this.props
        .loadSources(this.props.component, from, to, this.props.branch)
        .then(sources => resolve(sources), onFailLoadSources);
    });
  }

  loadSourcesBefore = () => {
    if (!this.state.sources) {
      return;
    }
    const firstSourceLine = this.state.sources[0];
    this.setState({ loadingSourcesBefore: true });
    const from = Math.max(1, firstSourceLine.line - LINES);
    this.props
      .loadSources(this.props.component, from, firstSourceLine.line - 1, this.props.branch)
      .then(sources => {
        this.props.loadIssues(this.props.component, from, firstSourceLine.line - 1).then(issues => {
          if (this.mounted) {
            this.setState(prevState => {
              const nextIssues = uniqBy([...issues, ...prevState.issues], issue => issue.key);
              return {
                issues: nextIssues,
                issuesByLine: issuesByLine(nextIssues),
                issueLocationsByLine: locationsByLine(nextIssues),
                loadingSourcesBefore: false,
                sources: [...this.computeCoverageStatus(sources), ...prevState.sources],
                symbolsByLine: { ...prevState.symbolsByLine, ...symbolsByLine(sources) }
              };
            });
          }
        });
      });
  };

  loadSourcesAfter = () => {
    if (!this.state.sources) {
      return;
    }
    const lastSourceLine = this.state.sources[this.state.sources.length - 1];
    this.setState({ loadingSourcesAfter: true });
    const fromLine = lastSourceLine.line + 1;
    // request one additional line to define `hasSourcesAfter`
    const toLine = lastSourceLine.line + LINES + 1;
    this.props
      .loadSources(this.props.component, fromLine, toLine, this.props.branch)
      .then(sources => {
        this.props.loadIssues(this.props.component, fromLine, toLine).then(issues => {
          if (this.mounted) {
            this.setState(prevState => {
              const nextIssues = uniqBy([...prevState.issues, ...issues], issue => issue.key);
              return {
                issues: nextIssues,
                issuesByLine: issuesByLine(nextIssues),
                issueLocationsByLine: locationsByLine(nextIssues),
                hasSourcesAfter: sources.length > LINES,
                loadingSourcesAfter: false,
                sources: [
                  ...prevState.sources,
                  ...this.computeCoverageStatus(sources.slice(0, LINES))
                ],
                symbolsByLine: {
                  ...prevState.symbolsByLine,
                  ...symbolsByLine(sources.slice(0, LINES))
                }
              };
            });
          }
        });
      });
  };

  loadDuplications = (line /*: SourceLine */) => {
    getDuplications(this.props.component, this.props.branch).then(r => {
      if (this.mounted) {
        this.setState(
          {
            displayDuplications: true,
            duplications: r.duplications,
            duplicationsByLine: duplicationsByLine(r.duplications),
            duplicatedFiles: r.files
          },
          () => {
            // immediately show dropdown popup if there is only one duplicated block
            if (r.duplications.length === 1) {
              this.handleDuplicationClick(0, line.line);
            }
          }
        );
      }
    });
  };

  showMeasures = () => {
    const measuresOverlay = new MeasuresOverlay({
      branch: this.props.branch,
      component: this.state.component,
      large: true
    });
    measuresOverlay.render();
  };

  handleCoverageClick = (line /*: SourceLine */, element /*: HTMLElement */) => {
    getTests(this.props.component, line.line, this.props.branch).then(tests => {
      const popup = new CoveragePopupView({
        line,
        tests,
        triggerEl: element,
        branch: this.props.branch
      });
      popup.render();
    });
  };

  handleDuplicationClick = (index /*: number */, line /*: number */) => {
    const duplication = this.state.duplications && this.state.duplications[index];
    let blocks = (duplication && duplication.blocks) || [];
    const inRemovedComponent = blocks.some(b => b._ref == null);
    let foundOne = false;
    blocks = blocks.filter(b => {
      const outOfBounds = b.from > line || b.from + b.size < line;
      const currentFile = b._ref === '1';
      const shouldDisplayForCurrentFile = outOfBounds || foundOne;
      const shouldDisplay = !currentFile || shouldDisplayForCurrentFile;
      const isOk = b._ref != null && shouldDisplay;
      if (b._ref === '1' && !outOfBounds) {
        foundOne = true;
      }
      return isOk;
    });

    const element = this.node.querySelector(
      `.source-line-duplications-extra[data-line-number="${line}"]`
    );
    if (element) {
      const popup = new DuplicationPopupView({
        blocks,
        inRemovedComponent,
        component: this.state.component,
        files: this.state.duplicatedFiles,
        triggerEl: element,
        branch: this.props.branch
      });
      popup.render();
    }
  };

  handlePopupToggle = (issue /*: string */, popupName /*: string */, open /*: ?boolean */) => {
    this.setState((state /*: State */) => {
      const samePopup =
        state.openPopup && state.openPopup.name === popupName && state.openPopup.issue === issue;
      if (open !== false && !samePopup) {
        return { openPopup: { issue, name: popupName } };
      } else if (open !== true && samePopup) {
        return { openPopup: null };
      }
      return state;
    });
  };

  displayLinePopup(line /*: number */, element /*: HTMLElement */) {
    const popup = new LineActionsPopupView({
      line,
      triggerEl: element,
      component: this.state.component,
      branch: this.props.branch
    });
    popup.render();
  }

  handleLineClick = (line /*: SourceLine */, element /*: HTMLElement */) => {
    this.setState(prevState => ({
      highlightedLine: prevState.highlightedLine === line.line ? null : line
    }));
    this.displayLinePopup(line.line, element);
  };

  handleSymbolClick = (symbols /*: Array<string> */) => {
    this.setState(state => {
      const shouldDisable = intersection(state.highlightedSymbols, symbols).length > 0;
      const highlightedSymbols = shouldDisable ? [] : symbols;
      return { highlightedSymbols };
    });
  };

  handleSCMClick = (line /*: SourceLine */, element /*: HTMLElement */) => {
    const popup = new SCMPopupView({ triggerEl: element, line });
    popup.render();
  };

  handleIssueSelect = (issue /*: string */) => {
    if (this.props.onIssueSelect) {
      this.props.onIssueSelect(issue);
    } else {
      this.setState({ selectedIssue: issue });
    }
  };

  handleIssueUnselect = () => {
    if (this.props.onIssueUnselect) {
      this.props.onIssueUnselect();
    } else {
      this.setState({ selectedIssue: undefined });
    }
  };

  handleOpenIssues = (line /*: SourceLine */) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: true }
    }));
  };

  handleCloseIssues = (line /*: SourceLine */) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: false }
    }));
  };

  handleIssueChange = (issue /*: Issue */) => {
    this.setState(state => {
      const issues = state.issues.map(
        candidate => (candidate.key === issue.key ? issue : candidate)
      );
      return { issues, issuesByLine: issuesByLine(issues) };
    });
    if (this.props.onIssueChange) {
      this.props.onIssueChange(issue);
    }
  };

  handleFilterLine = (line /*: SourceLine */) => {
    const { component } = this.state;
    const leakPeriodDate = component && component.leakPeriodDate;
    return leakPeriodDate
      ? line.scmDate != null && parseDate(line.scmDate) > leakPeriodDate
      : false;
  };

  renderCode(sources /*: Array<SourceLine> */) {
    const hasSourcesBefore = sources.length > 0 && sources[0].line > 1;
    return (
      <SourceViewerCode
        branch={this.props.branch}
        displayAllIssues={this.props.displayAllIssues}
        displayIssueLocationsCount={this.props.displayIssueLocationsCount}
        displayIssueLocationsLink={this.props.displayIssueLocationsLink}
        displayLocationMarkers={this.props.displayLocationMarkers}
        duplications={this.state.duplications}
        duplicationsByLine={this.state.duplicationsByLine}
        duplicatedFiles={this.state.duplicatedFiles}
        hasSourcesBefore={hasSourcesBefore}
        hasSourcesAfter={this.state.hasSourcesAfter}
        filterLine={this.handleFilterLine}
        highlightedLine={this.state.highlightedLine}
        highlightedLocations={this.props.highlightedLocations}
        highlightedLocationMessage={this.props.highlightedLocationMessage}
        highlightedSymbols={this.state.highlightedSymbols}
        issues={this.state.issues}
        issuesByLine={this.state.issuesByLine}
        issueLocationsByLine={this.state.issueLocationsByLine}
        loadDuplications={this.loadDuplications}
        loadSourcesAfter={this.loadSourcesAfter}
        loadSourcesBefore={this.loadSourcesBefore}
        loadingSourcesAfter={this.state.loadingSourcesAfter}
        loadingSourcesBefore={this.state.loadingSourcesBefore}
        onCoverageClick={this.handleCoverageClick}
        onDuplicationClick={this.handleDuplicationClick}
        onIssueChange={this.handleIssueChange}
        onIssueSelect={this.handleIssueSelect}
        onIssueUnselect={this.handleIssueUnselect}
        onIssuesOpen={this.handleOpenIssues}
        onIssuesClose={this.handleCloseIssues}
        onLineClick={this.handleLineClick}
        onLocationSelect={this.props.onLocationSelect}
        onPopupToggle={this.handlePopupToggle}
        openPopup={this.state.openPopup}
        onSCMClick={this.handleSCMClick}
        onSymbolClick={this.handleSymbolClick}
        openIssuesByLine={this.state.openIssuesByLine}
        scroll={this.props.scroll}
        selectedIssue={this.state.selectedIssue}
        sources={sources}
        symbolsByLine={this.state.symbolsByLine}
      />
    );
  }

  render() {
    const { component, loading, sources, notAccessible, sourceRemoved } = this.state;

    if (loading) {
      return null;
    }

    if (this.state.notExist) {
      return (
        <div className="alert alert-warning spacer-top">
          {translate('component_viewer.no_component')}
        </div>
      );
    }

    if (notAccessible) {
      return (
        <div className="alert alert-warning spacer-top">
          {translate('code_viewer.no_source_code_displayed_due_to_security')}
        </div>
      );
    }

    if (component == null) {
      return null;
    }

    const className = classNames('source-viewer', {
      'source-duplications-expanded': this.state.displayDuplications
    });

    return (
      <div className={className} ref={node => (this.node = node)}>
        <SourceViewerHeader
          branch={this.props.branch}
          component={this.state.component}
          showMeasures={this.showMeasures}
        />
        {sourceRemoved && (
          <div className="alert alert-warning spacer-top">
            {translate('code_viewer.no_source_code_displayed_due_to_source_removed')}
          </div>
        )}
        {!sourceRemoved && sources != null && this.renderCode(sources)}
      </div>
    );
  }
}
