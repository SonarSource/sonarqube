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
import { intersection, uniqBy } from 'lodash';
import * as React from 'react';
import {
  getComponentData,
  getComponentForSourceViewer,
  getDuplications,
  getSources
} from '../../api/components';
import { Alert } from '../../components/ui/Alert';
import { getBranchLikeQuery, isSameBranchLike } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { BranchLike } from '../../types/branch-like';
import {
  Dict,
  DuplicatedFile,
  Duplication,
  FlowLocation,
  Issue,
  LinearIssueLocation,
  Measure,
  SourceLine,
  SourceViewerFile
} from '../../types/types';
import { WorkspaceContext } from '../workspace/context';
import DuplicationPopup from './components/DuplicationPopup';
import {
  filterDuplicationBlocksByLine,
  getDuplicationBlocksForIndex,
  isDuplicationBlockInRemovedComponent
} from './helpers/duplications';
import getCoverageStatus from './helpers/getCoverageStatus';
import {
  duplicationsByLine,
  issuesByLine,
  locationsByLine,
  symbolsByLine
} from './helpers/indexing';
import defaultLoadIssues from './helpers/loadIssues';
import SourceViewerCode from './SourceViewerCode';
import { SourceViewerContext } from './SourceViewerContext';
import SourceViewerHeader from './SourceViewerHeader';
import SourceViewerHeaderSlim from './SourceViewerHeaderSlim';
import './styles.css';

// TODO react-virtualized

export interface Props {
  aroundLine?: number;
  branchLike: BranchLike | undefined;
  component: string;
  componentMeasures?: Measure[];
  displayAllIssues?: boolean;
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayLocationMarkers?: boolean;
  highlightedLine?: number;
  // `undefined` elements mean they are located in a different file,
  // but kept to maintaint the location indexes
  highlightedLocations?: (FlowLocation | undefined)[];
  highlightedLocationMessage?: { index: number; text: string | undefined };
  loadComponent?: (
    component: string,
    branchLike: BranchLike | undefined
  ) => Promise<SourceViewerFile>;
  loadIssues?: (
    component: string,
    from: number,
    to: number,
    branchLike: BranchLike | undefined
  ) => Promise<Issue[]>;
  loadSources?: (
    component: string,
    from: number,
    to: number,
    branchLike: BranchLike | undefined
  ) => Promise<SourceLine[]>;
  onLoaded?: (component: SourceViewerFile, sources: SourceLine[], issues: Issue[]) => void;
  onLocationSelect?: (index: number) => void;
  onIssueChange?: (issue: Issue) => void;
  onIssueSelect?: (issueKey: string) => void;
  onIssueUnselect?: () => void;
  scroll?: (element: HTMLElement) => void;
  selectedIssue?: string;
  showMeasures?: boolean;
  metricKey?: string;
  slimHeader?: boolean;
}

interface State {
  component?: SourceViewerFile;
  duplicatedFiles?: Dict<DuplicatedFile>;
  duplications?: Duplication[];
  duplicationsByLine: { [line: number]: number[] };
  hasSourcesAfter: boolean;
  highlightedSymbols: string[];
  issueLocationsByLine: { [line: number]: LinearIssueLocation[] };
  issuePopup?: { issue: string; name: string };
  issues?: Issue[];
  issuesByLine: { [line: number]: Issue[] };
  loading: boolean;
  loadingSourcesAfter: boolean;
  loadingSourcesBefore: boolean;
  notAccessible: boolean;
  notExist: boolean;
  openIssuesByLine: { [line: number]: boolean };
  selectedIssue?: string;
  sourceRemoved: boolean;
  sources?: SourceLine[];
  symbolsByLine: { [line: number]: string[] };
}

const LINES = 500;

export default class SourceViewerBase extends React.PureComponent<Props, State> {
  node?: HTMLElement | null;
  mounted = false;

  static defaultProps = {
    displayAllIssues: false,
    displayIssueLocationsCount: true,
    displayIssueLocationsLink: true,
    displayLocationMarkers: true
  };

  constructor(props: Props) {
    super(props);

    this.state = {
      duplicationsByLine: {},
      hasSourcesAfter: false,
      highlightedSymbols: [],
      issuesByLine: {},
      issueLocationsByLine: {},
      loading: true,
      loadingSourcesAfter: false,
      loadingSourcesBefore: false,
      notAccessible: false,
      notExist: false,
      openIssuesByLine: {},
      selectedIssue: props.selectedIssue,
      sourceRemoved: false,
      symbolsByLine: {}
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();
  }

  componentWillReceiveProps(nextProps: Props) {
    // if a component or a branch has changed,
    // set `loading: true` immediately to avoid unwanted scrolling in `LineCode`
    if (
      nextProps.component !== this.props.component ||
      !isSameBranchLike(nextProps.branchLike, this.props.branchLike)
    ) {
      this.setState({ loading: true });
    }
    if (
      nextProps.onIssueSelect !== undefined &&
      nextProps.selectedIssue !== this.props.selectedIssue
    ) {
      this.setState({ selectedIssue: nextProps.selectedIssue });
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.component !== this.props.component ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike)
    ) {
      this.fetchComponent();
    } else if (
      this.props.aroundLine !== undefined &&
      prevProps.aroundLine !== this.props.aroundLine &&
      this.isLineOutsideOfRange(this.props.aroundLine)
    ) {
      this.fetchSources();
    } else {
      const { selectedIssue } = this.props;
      const { issues } = this.state;
      if (
        selectedIssue !== undefined &&
        issues !== undefined &&
        issues.find(issue => issue.key === selectedIssue) === undefined
      ) {
        this.reloadIssues();
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  get loadComponent() {
    return this.props.loadComponent || defaultLoadComponent;
  }

  get loadIssues() {
    return this.props.loadIssues || defaultLoadIssues;
  }

  get propsLoadSources() {
    return this.props.loadSources || defaultLoadSources;
  }

  computeCoverageStatus(lines: SourceLine[]) {
    return lines.map(line => ({ ...line, coverageStatus: getCoverageStatus(line) }));
  }

  isLineOutsideOfRange(lineNumber: number) {
    const { sources } = this.state;
    if (sources && sources.length > 0) {
      const firstLine = sources[0];
      const lastList = sources[sources.length - 1];
      return lineNumber < firstLine.line || lineNumber > lastList.line;
    }

    return true;
  }

  fetchComponent() {
    this.setState({ loading: true });

    const to = (this.props.aroundLine || 0) + LINES;
    const loadIssues = (component: SourceViewerFile, sources: SourceLine[]) => {
      this.loadIssues(this.props.component, 1, to, this.props.branchLike).then(
        issues => {
          if (this.mounted) {
            const finalSources = sources.slice(0, LINES);

            this.setState(
              {
                component,
                duplicatedFiles: undefined,
                duplications: undefined,
                duplicationsByLine: {},
                hasSourcesAfter: sources.length > LINES,
                highlightedSymbols: [],
                issueLocationsByLine: locationsByLine(issues),
                issues,
                issuesByLine: issuesByLine(issues),
                loading: false,
                notAccessible: false,
                notExist: false,
                openIssuesByLine: {},
                issuePopup: undefined,
                sourceRemoved: false,
                sources: this.computeCoverageStatus(finalSources),
                symbolsByLine: symbolsByLine(sources.slice(0, LINES))
              },
              () => {
                if (this.props.onLoaded) {
                  this.props.onLoaded(component, finalSources, issues);
                }
              }
            );
          }
        },
        () => {
          // TODO
        }
      );
    };

    const onFailLoadComponent = (response: Response) => {
      // TODO handle other statuses
      if (this.mounted) {
        if (response.status === 403) {
          this.setState({ loading: false, notAccessible: true });
        } else if (response.status === 404) {
          this.setState({ loading: false, notExist: true });
        }
      }
    };

    const onFailLoadSources = (response: Response, component: SourceViewerFile) => {
      // TODO handle other statuses
      if (this.mounted) {
        if (response.status === 403) {
          this.setState({ component, loading: false, notAccessible: true });
        } else if (response.status === 404) {
          this.setState({ component, loading: false, sourceRemoved: true });
        }
      }
    };

    const onResolve = (component: SourceViewerFile) => {
      const sourcesRequest =
        component.q === 'FIL' || component.q === 'UTS' ? this.loadSources() : Promise.resolve([]);
      sourcesRequest.then(
        sources => loadIssues(component, sources),
        response => onFailLoadSources(response, component)
      );
    };

    this.loadComponent(this.props.component, this.props.branchLike).then(
      onResolve,
      onFailLoadComponent
    );
  }

  fetchSources() {
    this.loadSources().then(
      sources => {
        if (this.mounted) {
          const finalSources = sources.slice(0, LINES);
          this.setState(
            {
              sources: sources.slice(0, LINES),
              hasSourcesAfter: sources.length > LINES
            },
            () => {
              if (this.props.onLoaded && this.state.component && this.state.issues) {
                this.props.onLoaded(this.state.component, finalSources, this.state.issues);
              }
            }
          );
        }
      },
      () => {
        // TODO
      }
    );
  }

  reloadIssues() {
    if (!this.state.sources) {
      return;
    }
    const firstSourceLine = this.state.sources[0];
    const lastSourceLine = this.state.sources[this.state.sources.length - 1];
    this.loadIssues(
      this.props.component,
      firstSourceLine && firstSourceLine.line,
      lastSourceLine && lastSourceLine.line,
      this.props.branchLike
    ).then(
      issues => {
        if (this.mounted) {
          this.setState({
            issues,
            issuesByLine: issuesByLine(issues),
            issueLocationsByLine: locationsByLine(issues)
          });
        }
      },
      () => {
        // TODO
      }
    );
  }

  loadSources = (): Promise<SourceLine[]> => {
    return new Promise((resolve, reject) => {
      const onFailLoadSources = (response: Response) => {
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

      this.propsLoadSources(this.props.component, from, to, this.props.branchLike).then(sources => {
        resolve(sources);
      }, onFailLoadSources);
    });
  };

  loadSourcesBefore = () => {
    if (!this.state.sources) {
      return;
    }
    const firstSourceLine = this.state.sources[0];
    this.setState({ loadingSourcesBefore: true });
    const from = Math.max(1, firstSourceLine.line - LINES);
    Promise.all([
      this.propsLoadSources(
        this.props.component,
        from,
        firstSourceLine.line - 1,
        this.props.branchLike
      ),
      this.loadIssues(this.props.component, from, firstSourceLine.line - 1, this.props.branchLike)
    ]).then(
      ([sources, issues]) => {
        if (this.mounted) {
          this.setState(prevState => {
            const nextIssues = uniqBy([...issues, ...(prevState.issues || [])], issue => issue.key);
            return {
              issues: nextIssues,
              issuesByLine: issuesByLine(nextIssues),
              issueLocationsByLine: locationsByLine(nextIssues),
              loadingSourcesBefore: false,
              sources: [...this.computeCoverageStatus(sources), ...(prevState.sources || [])],
              symbolsByLine: { ...prevState.symbolsByLine, ...symbolsByLine(sources) }
            };
          });
        }
      },
      () => {
        // TODO
      }
    );
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
    Promise.all([
      this.propsLoadSources(this.props.component, fromLine, toLine, this.props.branchLike),
      this.loadIssues(this.props.component, fromLine, toLine, this.props.branchLike)
    ]).then(
      ([sources, issues]) => {
        if (this.mounted) {
          this.setState(prevState => {
            const nextIssues = uniqBy([...(prevState.issues || []), ...issues], issue => issue.key);
            return {
              issues: nextIssues,
              issuesByLine: issuesByLine(nextIssues),
              issueLocationsByLine: locationsByLine(nextIssues),
              hasSourcesAfter: sources.length > LINES,
              loadingSourcesAfter: false,
              sources: [
                ...(prevState.sources || []),
                ...this.computeCoverageStatus(sources.slice(0, LINES))
              ],
              symbolsByLine: {
                ...prevState.symbolsByLine,
                ...symbolsByLine(sources.slice(0, LINES))
              }
            };
          });
        }
      },
      () => {
        // TODO
      }
    );
  };

  loadDuplications = () => {
    getDuplications({
      key: this.props.component,
      ...getBranchLikeQuery(this.props.branchLike)
    }).then(
      r => {
        if (this.mounted) {
          this.setState({
            duplications: r.duplications,
            duplicationsByLine: duplicationsByLine(r.duplications),
            duplicatedFiles: r.files
          });
        }
      },
      () => {
        // TODO
      }
    );
  };

  handleIssuePopupToggle = (issue: string, popupName: string, open?: boolean) => {
    this.setState((state: State) => {
      const samePopup =
        state.issuePopup && state.issuePopup.name === popupName && state.issuePopup.issue === issue;
      if (open !== false && !samePopup) {
        return { issuePopup: { issue, name: popupName } };
      } else if (open !== true && samePopup) {
        return { issuePopup: undefined };
      }
      return null;
    });
  };

  handleSymbolClick = (symbols: string[]) => {
    this.setState(state => {
      const shouldDisable = intersection(state.highlightedSymbols, symbols).length > 0;
      const highlightedSymbols = shouldDisable ? [] : symbols;
      return { highlightedSymbols };
    });
  };

  handleIssueSelect = (issue: string) => {
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

  handleOpenIssues = (line: SourceLine) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: true }
    }));
  };

  handleCloseIssues = (line: SourceLine) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: false }
    }));
  };

  handleIssueChange = (issue: Issue) => {
    this.setState(({ issues = [] }) => {
      const newIssues = issues.map(candidate => (candidate.key === issue.key ? issue : candidate));
      return { issues: newIssues, issuesByLine: issuesByLine(newIssues) };
    });
    if (this.props.onIssueChange) {
      this.props.onIssueChange(issue);
    }
  };

  renderDuplicationPopup = (index: number, line: number) => {
    const { component, duplicatedFiles, duplications } = this.state;

    if (!component || !duplicatedFiles) {
      return null;
    }

    const blocks = getDuplicationBlocksForIndex(duplications, index);

    return (
      <WorkspaceContext.Consumer>
        {({ openComponent }) => (
          <DuplicationPopup
            blocks={filterDuplicationBlocksByLine(blocks, line)}
            branchLike={this.props.branchLike}
            duplicatedFiles={duplicatedFiles}
            inRemovedComponent={isDuplicationBlockInRemovedComponent(blocks)}
            openComponent={openComponent}
            sourceViewerFile={component}
          />
        )}
      </WorkspaceContext.Consumer>
    );
  };

  renderCode(sources: SourceLine[]) {
    const hasSourcesBefore = sources.length > 0 && sources[0].line > 1;
    return (
      <SourceViewerCode
        branchLike={this.props.branchLike}
        componentKey={this.props.component}
        displayAllIssues={this.props.displayAllIssues}
        displayIssueLocationsCount={this.props.displayIssueLocationsCount}
        displayIssueLocationsLink={this.props.displayIssueLocationsLink}
        displayLocationMarkers={this.props.displayLocationMarkers}
        duplications={this.state.duplications}
        duplicationsByLine={this.state.duplicationsByLine}
        hasSourcesAfter={this.state.hasSourcesAfter}
        hasSourcesBefore={hasSourcesBefore}
        highlightedLine={this.props.highlightedLine}
        highlightedLocationMessage={this.props.highlightedLocationMessage}
        highlightedLocations={this.props.highlightedLocations}
        highlightedSymbols={this.state.highlightedSymbols}
        issueLocationsByLine={this.state.issueLocationsByLine}
        issuePopup={this.state.issuePopup}
        issues={this.state.issues}
        issuesByLine={this.state.issuesByLine}
        loadDuplications={this.loadDuplications}
        loadSourcesAfter={this.loadSourcesAfter}
        loadSourcesBefore={this.loadSourcesBefore}
        loadingSourcesAfter={this.state.loadingSourcesAfter}
        loadingSourcesBefore={this.state.loadingSourcesBefore}
        onIssueChange={this.handleIssueChange}
        onIssuePopupToggle={this.handleIssuePopupToggle}
        onIssueSelect={this.handleIssueSelect}
        onIssueUnselect={this.handleIssueUnselect}
        onIssuesClose={this.handleCloseIssues}
        onIssuesOpen={this.handleOpenIssues}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={this.handleSymbolClick}
        openIssuesByLine={this.state.openIssuesByLine}
        renderDuplicationPopup={this.renderDuplicationPopup}
        scroll={this.props.scroll}
        metricKey={this.props.metricKey}
        selectedIssue={this.state.selectedIssue}
        sources={sources}
        symbolsByLine={this.state.symbolsByLine}
      />
    );
  }

  renderHeader(branchLike: BranchLike | undefined, sourceViewerFile: SourceViewerFile) {
    return this.props.slimHeader ? (
      <SourceViewerHeaderSlim branchLike={branchLike} sourceViewerFile={sourceViewerFile} />
    ) : (
      <WorkspaceContext.Consumer>
        {({ openComponent }) => (
          <SourceViewerHeader
            branchLike={this.props.branchLike}
            componentMeasures={this.props.componentMeasures}
            openComponent={openComponent}
            showMeasures={this.props.showMeasures}
            sourceViewerFile={sourceViewerFile}
          />
        )}
      </WorkspaceContext.Consumer>
    );
  }

  render() {
    const { component, loading, sources, notAccessible, sourceRemoved } = this.state;

    if (loading) {
      return null;
    }

    if (this.state.notExist) {
      return (
        <Alert className="spacer-top" variant="warning">
          {translate('component_viewer.no_component')}
        </Alert>
      );
    }

    if (notAccessible) {
      return (
        <Alert className="spacer-top" variant="warning">
          {translate('code_viewer.no_source_code_displayed_due_to_security')}
        </Alert>
      );
    }

    if (!component) {
      return null;
    }

    return (
      <SourceViewerContext.Provider value={{ branchLike: this.props.branchLike, file: component }}>
        <div className="source-viewer" ref={node => (this.node = node)}>
          {this.renderHeader(this.props.branchLike, component)}
          {sourceRemoved && (
            <Alert className="spacer-top" variant="warning">
              {translate('code_viewer.no_source_code_displayed_due_to_source_removed')}
            </Alert>
          )}
          {!sourceRemoved && sources !== undefined && this.renderCode(sources)}
        </div>
      </SourceViewerContext.Provider>
    );
  }
}

function defaultLoadComponent(
  component: string,
  branchLike: BranchLike | undefined
): Promise<SourceViewerFile> {
  return Promise.all([
    getComponentForSourceViewer({ component, ...getBranchLikeQuery(branchLike) }),
    getComponentData({ component, ...getBranchLikeQuery(branchLike) })
  ]).then(([sourceViewerComponent, { component }]) => ({
    ...sourceViewerComponent,
    leakPeriodDate: component.leakPeriodDate
  }));
}

function defaultLoadSources(
  key: string,
  from: number | undefined,
  to: number | undefined,
  branchLike: BranchLike | undefined
) {
  return getSources({ key, from, to, ...getBranchLikeQuery(branchLike) });
}
