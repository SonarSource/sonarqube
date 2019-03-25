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
import * as classNames from 'classnames';
import { intersection, uniqBy } from 'lodash';
import SourceViewerHeader from './SourceViewerHeader';
import SourceViewerCode from './SourceViewerCode';
import { SourceViewerContext } from './SourceViewerContext';
import DuplicationPopup from './components/DuplicationPopup';
import defaultLoadIssues from './helpers/loadIssues';
import getCoverageStatus from './helpers/getCoverageStatus';
import {
  duplicationsByLine,
  issuesByLine,
  locationsByLine,
  symbolsByLine
} from './helpers/indexing';
import {
  getComponentData,
  getComponentForSourceViewer,
  getDuplications,
  getSources
} from '../../api/components';
import { isSameBranchLike, getBranchLikeQuery } from '../../helpers/branches';
import { translate } from '../../helpers/l10n';
import { Alert } from '../ui/Alert';
import { WorkspaceContext } from '../workspace/context';
import './styles.css';

// TODO react-virtualized

export interface Props {
  aroundLine?: number;
  branchLike: T.BranchLike | undefined;
  component: string;
  displayAllIssues?: boolean;
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayLocationMarkers?: boolean;
  highlightedLine?: number;
  // `undefined` elements mean they are located in a different file,
  // but kept to maintaint the location indexes
  highlightedLocations?: (T.FlowLocation | undefined)[];
  highlightedLocationMessage?: { index: number; text: string | undefined };
  loadComponent: (
    component: string,
    branchLike: T.BranchLike | undefined
  ) => Promise<T.SourceViewerFile>;
  loadIssues: (
    component: string,
    from: number,
    to: number,
    branchLike: T.BranchLike | undefined
  ) => Promise<T.Issue[]>;
  loadSources: (
    component: string,
    from: number,
    to: number,
    branchLike: T.BranchLike | undefined
  ) => Promise<T.SourceLine[]>;
  onLoaded?: (component: T.SourceViewerFile, sources: T.SourceLine[], issues: T.Issue[]) => void;
  onLocationSelect?: (index: number) => void;
  onIssueChange?: (issue: T.Issue) => void;
  onIssueSelect?: (issueKey: string) => void;
  onIssueUnselect?: () => void;
  scroll?: (element: HTMLElement) => void;
  selectedIssue?: string;
}

interface State {
  component?: T.SourceViewerFile;
  displayDuplications: boolean;
  duplicatedFiles?: T.Dict<T.DuplicatedFile>;
  duplications?: T.Duplication[];
  duplicationsByLine: { [line: number]: number[] };
  hasSourcesAfter: boolean;
  highlightedSymbols: string[];
  issueLocationsByLine: { [line: number]: T.LinearIssueLocation[] };
  issuePopup?: { issue: string; name: string };
  issues?: T.Issue[];
  issuesByLine: { [line: number]: T.Issue[] };
  linePopup?: { index?: number; line: number; name: string };
  loading: boolean;
  loadingSourcesAfter: boolean;
  loadingSourcesBefore: boolean;
  notAccessible: boolean;
  notExist: boolean;
  openIssuesByLine: { [line: number]: boolean };
  selectedIssue?: string;
  sourceRemoved: boolean;
  sources?: T.SourceLine[];
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
    displayLocationMarkers: true,
    loadComponent: defaultLoadComponent,
    loadIssues: defaultLoadIssues,
    loadSources: defaultLoadSources
  };

  constructor(props: Props) {
    super(props);

    this.state = {
      displayDuplications: false,
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

  computeCoverageStatus(lines: T.SourceLine[]) {
    return lines.map(line => ({ ...line, coverageStatus: getCoverageStatus(line) }));
  }

  isLineOutsideOfRange(lineNumber: number) {
    const { sources } = this.state;
    if (sources && sources.length > 0) {
      const firstLine = sources[0];
      const lastList = sources[sources.length - 1];
      return lineNumber < firstLine.line || lineNumber > lastList.line;
    } else {
      return true;
    }
  }

  fetchComponent() {
    this.setState({ loading: true });

    const to = (this.props.aroundLine || 0) + LINES;
    const loadIssues = (component: T.SourceViewerFile, sources: T.SourceLine[]) => {
      this.props.loadIssues(this.props.component, 1, to, this.props.branchLike).then(
        issues => {
          if (this.mounted) {
            const finalSources = sources.slice(0, LINES);

            this.setState(
              {
                component,
                displayDuplications: false,
                duplicatedFiles: undefined,
                duplications: undefined,
                duplicationsByLine: {},
                hasSourcesAfter: sources.length > LINES,
                highlightedSymbols: [],
                issueLocationsByLine: locationsByLine(issues),
                issues,
                issuesByLine: issuesByLine(issues),
                linePopup: undefined,
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

    const onFailLoadComponent = ({ response }: { response: Response }) => {
      // TODO handle other statuses
      if (this.mounted) {
        if (response.status === 403) {
          this.setState({ loading: false, notAccessible: true });
        } else if (response.status === 404) {
          this.setState({ loading: false, notExist: true });
        }
      }
    };

    const onFailLoadSources = (response: Response, component: T.SourceViewerFile) => {
      // TODO handle other statuses
      if (this.mounted) {
        if (response.status === 403) {
          this.setState({ component, loading: false, notAccessible: true });
        } else if (response.status === 404) {
          this.setState({ component, loading: false, sourceRemoved: true });
        }
      }
    };

    const onResolve = (component: T.SourceViewerFile) => {
      const sourcesRequest =
        component.q === 'FIL' || component.q === 'UTS' ? this.loadSources() : Promise.resolve([]);
      sourcesRequest.then(
        sources => loadIssues(component, sources),
        response => onFailLoadSources(response, component)
      );
    };

    this.props
      .loadComponent(this.props.component, this.props.branchLike)
      .then(onResolve, onFailLoadComponent);
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
    this.props
      .loadIssues(
        this.props.component,
        firstSourceLine && firstSourceLine.line,
        lastSourceLine && lastSourceLine.line,
        this.props.branchLike
      )
      .then(
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

  loadSources = (): Promise<T.SourceLine[]> => {
    return new Promise((resolve, reject) => {
      const onFailLoadSources = ({ response }: { response: Response }) => {
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
        .loadSources(this.props.component, from, to, this.props.branchLike)
        .then(sources => resolve(sources), onFailLoadSources);
    });
  };

  loadSourcesBefore = () => {
    if (!this.state.sources) {
      return;
    }
    const firstSourceLine = this.state.sources[0];
    this.setState({ loadingSourcesBefore: true });
    const from = Math.max(1, firstSourceLine.line - LINES);
    this.props
      .loadSources(this.props.component, from, firstSourceLine.line - 1, this.props.branchLike)
      .then(
        sources => {
          this.props
            .loadIssues(this.props.component, from, firstSourceLine.line - 1, this.props.branchLike)
            .then(
              issues => {
                if (this.mounted) {
                  this.setState(prevState => {
                    const nextIssues = uniqBy(
                      [...issues, ...(prevState.issues || [])],
                      issue => issue.key
                    );
                    return {
                      issues: nextIssues,
                      issuesByLine: issuesByLine(nextIssues),
                      issueLocationsByLine: locationsByLine(nextIssues),
                      loadingSourcesBefore: false,
                      sources: [
                        ...this.computeCoverageStatus(sources),
                        ...(prevState.sources || [])
                      ],
                      symbolsByLine: { ...prevState.symbolsByLine, ...symbolsByLine(sources) }
                    };
                  });
                }
              },
              () => {
                // TODO
              }
            );
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
    this.props.loadSources(this.props.component, fromLine, toLine, this.props.branchLike).then(
      sources => {
        this.props.loadIssues(this.props.component, fromLine, toLine, this.props.branchLike).then(
          issues => {
            if (this.mounted) {
              this.setState(prevState => {
                const nextIssues = uniqBy(
                  [...(prevState.issues || []), ...issues],
                  issue => issue.key
                );
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
      },
      () => {
        // TODO
      }
    );
  };

  loadDuplications = (line: T.SourceLine) => {
    getDuplications({
      key: this.props.component,
      ...getBranchLikeQuery(this.props.branchLike)
    }).then(
      r => {
        if (this.mounted) {
          this.setState(state => ({
            displayDuplications: true,
            duplications: r.duplications,
            duplicationsByLine: duplicationsByLine(r.duplications),
            duplicatedFiles: r.files,
            linePopup:
              r.duplications.length === 1
                ? { index: 0, line: line.line, name: 'duplications' }
                : state.linePopup
          }));
        }
      },
      () => {
        // TODO
      }
    );
  };

  handleLinePopupToggle = ({
    index,
    line,
    name,
    open
  }: {
    index?: number;
    line: number;
    name: string;
    open?: boolean;
  }) => {
    this.setState((state: State) => {
      const samePopup =
        state.linePopup !== undefined &&
        state.linePopup.name === name &&
        state.linePopup.line === line &&
        state.linePopup.index === index;
      if (open !== false && !samePopup) {
        return { linePopup: { index, line, name } };
      } else if (open !== true && samePopup) {
        return { linePopup: undefined };
      }
      return null;
    });
  };

  closeLinePopup = () => {
    this.setState({ linePopup: undefined });
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

  handleOpenIssues = (line: T.SourceLine) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: true }
    }));
  };

  handleCloseIssues = (line: T.SourceLine) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: false }
    }));
  };

  handleIssueChange = (issue: T.Issue) => {
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

    if (!component || !duplicatedFiles) return <></>;

    const duplication = duplications && duplications[index];
    let blocks = (duplication && duplication.blocks) || [];
    /* eslint-disable no-underscore-dangle */
    const inRemovedComponent = blocks.some(b => b._ref === undefined);
    let foundOne = false;
    blocks = blocks.filter(b => {
      const outOfBounds = b.from > line || b.from + b.size < line;
      const currentFile = b._ref === '1';
      const shouldDisplayForCurrentFile = outOfBounds || foundOne;
      const shouldDisplay = !currentFile || shouldDisplayForCurrentFile;
      const isOk = b._ref !== undefined && shouldDisplay;
      if (b._ref === '1' && !outOfBounds) {
        foundOne = true;
      }
      return isOk;
    });
    /* eslint-enable no-underscore-dangle */

    return (
      <WorkspaceContext.Consumer>
        {({ openComponent }) => (
          <DuplicationPopup
            blocks={blocks}
            branchLike={this.props.branchLike}
            duplicatedFiles={duplicatedFiles}
            inRemovedComponent={inRemovedComponent}
            onClose={this.closeLinePopup}
            openComponent={openComponent}
            sourceViewerFile={component}
          />
        )}
      </WorkspaceContext.Consumer>
    );
  };

  renderCode(sources: T.SourceLine[]) {
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
        linePopup={this.state.linePopup}
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
        onLinePopupToggle={this.handleLinePopupToggle}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={this.handleSymbolClick}
        openIssuesByLine={this.state.openIssuesByLine}
        renderDuplicationPopup={this.renderDuplicationPopup}
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

    const className = classNames('source-viewer', {
      'source-duplications-expanded': this.state.displayDuplications
    });

    return (
      <SourceViewerContext.Provider value={{ branchLike: this.props.branchLike, file: component }}>
        <div className={className} ref={node => (this.node = node)}>
          <WorkspaceContext.Consumer>
            {({ openComponent }) => (
              <SourceViewerHeader
                branchLike={this.props.branchLike}
                openComponent={openComponent}
                sourceViewerFile={component}
              />
            )}
          </WorkspaceContext.Consumer>
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

function defaultLoadComponent(component: string, branchLike: T.BranchLike | undefined) {
  return Promise.all([
    getComponentForSourceViewer({ component, ...getBranchLikeQuery(branchLike) }),
    getComponentData({ component, ...getBranchLikeQuery(branchLike) })
  ]).then(([component, data]) => ({
    ...component,
    leakPeriodDate: data.leakPeriodDate
  }));
}

function defaultLoadSources(
  key: string,
  from: number | undefined,
  to: number | undefined,
  branchLike: T.BranchLike | undefined
) {
  return getSources({ key, from, to, ...getBranchLikeQuery(branchLike) });
}
