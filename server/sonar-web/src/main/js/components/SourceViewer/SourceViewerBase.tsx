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
import * as React from 'react';
import * as classNames from 'classnames';
import { intersection, uniqBy } from 'lodash';
import SourceViewerHeader from './SourceViewerHeader';
import SourceViewerCode from './SourceViewerCode';
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
import {
  Duplication,
  FlowLocation,
  Issue,
  LinearIssueLocation,
  SourceLine,
  SourceViewerFile,
  DuplicatedFile
} from '../../app/types';
import { parseDate } from '../../helpers/dates';
import { translate } from '../../helpers/l10n';
import './styles.css';

// TODO react-virtualized

interface Props {
  aroundLine?: number;
  branch: string | undefined;
  component: string;
  displayAllIssues?: boolean;
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  displayLocationMarkers?: boolean;
  highlightedLine?: number;
  highlightedLocations?: FlowLocation[];
  highlightedLocationMessage?: { index: number; text: string };
  loadComponent?: (component: string, branch: string | undefined) => Promise<SourceViewerFile>;
  loadIssues?: (
    component: string,
    from: number,
    to: number,
    branch: string | undefined
  ) => Promise<Issue[]>;
  loadSources?: (
    component: string,
    from: number,
    to: number,
    branch: string | undefined
  ) => Promise<SourceLine[]>;
  onLoaded?: (component: SourceViewerFile, sources: SourceLine[], issues: Issue[]) => void;
  onLocationSelect?: (index: number) => void;
  onIssueChange?: (issue: Issue) => void;
  onIssueSelect?: (issueKey: string) => void;
  onIssueUnselect?: () => void;
  onReceiveComponent: (component: SourceViewerFile) => void;
  scroll?: (element: HTMLElement) => void;
  selectedIssue?: string;
}

interface State {
  component?: SourceViewerFile;
  displayDuplications: boolean;
  duplications?: Duplication[];
  duplicationsByLine: { [line: number]: number[] };
  duplicatedFiles?: { [ref: string]: DuplicatedFile };
  hasSourcesAfter: boolean;
  highlightedLine?: number;
  highlightedSymbols: string[];
  issues?: Issue[];
  issuesByLine: { [line: number]: Issue[] };
  issueLocationsByLine: { [line: number]: LinearIssueLocation[] };
  linePopup?: { index?: number; line: number; name: string };
  loading: boolean;
  loadingSourcesAfter: boolean;
  loadingSourcesBefore: boolean;
  notAccessible: boolean;
  notExist: boolean;
  openIssuesByLine: { [line: number]: boolean };
  issuePopup?: { issue: string; name: string };
  selectedIssue?: string;
  sources?: SourceLine[];
  sourceRemoved: boolean;
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
      displayDuplications: false,
      duplicationsByLine: {},
      hasSourcesAfter: false,
      highlightedLine: props.highlightedLine,
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
    if (
      nextProps.onIssueSelect !== undefined &&
      nextProps.selectedIssue !== this.props.selectedIssue
    ) {
      this.setState({ selectedIssue: nextProps.selectedIssue });
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component || prevProps.branch !== this.props.branch) {
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

  // react typings do not take `defaultProps` into account,
  // so use these getters to get type-safe methods

  get safeLoadComponent() {
    return this.props.loadComponent || defaultLoadComponent;
  }

  get safeLoadIssues() {
    return this.props.loadIssues || defaultLoadIssues;
  }

  get safeLoadSources() {
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
    } else {
      return true;
    }
  }

  fetchComponent() {
    this.setState({ loading: true });
    const loadIssues = (component: SourceViewerFile, sources: SourceLine[]) => {
      this.safeLoadIssues(this.props.component, 1, LINES, this.props.branch).then(
        issues => {
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
      this.props.onReceiveComponent(component);
      const sourcesRequest =
        component.q === 'FIL' || component.q === 'UTS' ? this.loadSources() : Promise.resolve([]);
      sourcesRequest.then(
        sources => loadIssues(component, sources),
        response => onFailLoadSources(response, component)
      );
    };

    this.safeLoadComponent(this.props.component, this.props.branch).then(
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
    this.safeLoadIssues(
      this.props.component,
      firstSourceLine && firstSourceLine.line,
      lastSourceLine && lastSourceLine.line,
      this.props.branch
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

      return this.safeLoadSources(this.props.component, from, to, this.props.branch).then(
        sources => resolve(sources),
        onFailLoadSources
      );
    });
  };

  loadSourcesBefore = () => {
    if (!this.state.sources) {
      return;
    }
    const firstSourceLine = this.state.sources[0];
    this.setState({ loadingSourcesBefore: true });
    const from = Math.max(1, firstSourceLine.line - LINES);
    this.safeLoadSources(
      this.props.component,
      from,
      firstSourceLine.line - 1,
      this.props.branch
    ).then(
      sources => {
        this.safeLoadIssues(
          this.props.component,
          from,
          firstSourceLine.line - 1,
          this.props.branch
        ).then(
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
    this.safeLoadSources(this.props.component, fromLine, toLine, this.props.branch).then(
      sources => {
        this.safeLoadIssues(this.props.component, fromLine, toLine, this.props.branch).then(
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

  loadDuplications = (line: SourceLine) => {
    getDuplications(this.props.component, this.props.branch).then(
      r => {
        if (this.mounted) {
          this.setState(() => {
            const changes: Partial<State> = {
              displayDuplications: true,
              duplications: r.duplications,
              duplicationsByLine: duplicationsByLine(r.duplications),
              duplicatedFiles: r.files
            };
            if (r.duplications.length === 1) {
              changes.linePopup = { index: 0, line: line.line, name: 'duplications' };
            }
            return changes;
          });
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

  handleFilterLine = (line: SourceLine) => {
    const { component } = this.state;
    const leakPeriodDate = component && component.leakPeriodDate;
    return leakPeriodDate
      ? line.scmDate !== undefined && parseDate(line.scmDate) > parseDate(leakPeriodDate)
      : false;
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
      <DuplicationPopup
        blocks={blocks}
        branch={this.props.branch}
        duplicatedFiles={duplicatedFiles}
        inRemovedComponent={inRemovedComponent}
        onClose={this.closeLinePopup}
        sourceViewerFile={component}
      />
    );
  };

  renderCode(sources: SourceLine[]) {
    const hasSourcesBefore = sources.length > 0 && sources[0].line > 1;
    return (
      <SourceViewerCode
        branch={this.props.branch}
        componentKey={this.props.component}
        displayAllIssues={this.props.displayAllIssues}
        displayIssueLocationsCount={this.props.displayIssueLocationsCount}
        displayIssueLocationsLink={this.props.displayIssueLocationsLink}
        displayLocationMarkers={this.props.displayLocationMarkers}
        duplications={this.state.duplications}
        duplicationsByLine={this.state.duplicationsByLine}
        filterLine={this.handleFilterLine}
        hasSourcesAfter={this.state.hasSourcesAfter}
        hasSourcesBefore={hasSourcesBefore}
        highlightedLine={this.state.highlightedLine}
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

    if (!component) {
      return null;
    }

    const className = classNames('source-viewer', {
      'source-duplications-expanded': this.state.displayDuplications
    });

    return (
      <div className={className} ref={node => (this.node = node)}>
        {this.state.component && (
          <SourceViewerHeader branch={this.props.branch} sourceViewerFile={this.state.component} />
        )}
        {sourceRemoved && (
          <div className="alert alert-warning spacer-top">
            {translate('code_viewer.no_source_code_displayed_due_to_source_removed')}
          </div>
        )}
        {!sourceRemoved && sources !== undefined && this.renderCode(sources)}
      </div>
    );
  }
}

function defaultLoadComponent(key: string, branch: string | undefined) {
  return Promise.all([
    getComponentForSourceViewer(key, branch),
    getComponentData(key, branch)
  ]).then(([component, data]) => ({
    ...component,
    leakPeriodDate: data.leakPeriodDate && parseDate(data.leakPeriodDate)
  }));
}

function defaultLoadSources(
  key: string,
  from: number | undefined,
  to: number | undefined,
  branch: string | undefined
) {
  return getSources(key, from, to, branch);
}
