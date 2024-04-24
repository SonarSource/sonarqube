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

import { FlagMessage } from 'design-system';
import { intersection } from 'lodash';
import * as React from 'react';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import {
  getComponentData,
  getComponentForSourceViewer,
  getDuplications,
  getSources,
} from '../../api/components';
import { ComponentContext } from '../../app/components/componentContext/ComponentContext';
import { isSameBranchLike } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { HttpStatus } from '../../helpers/request';
import { BranchLike } from '../../types/branch-like';
import { ComponentQualifier } from '../../types/component';
import {
  Dict,
  DuplicatedFile,
  Duplication,
  FlowLocation,
  Issue,
  LinearIssueLocation,
  Measure,
  SourceLine,
  SourceViewerFile,
} from '../../types/types';
import { WorkspaceContext } from '../workspace/context';
import SourceViewerCode from './SourceViewerCode';
import { SourceViewerContext } from './SourceViewerContext';
import SourceViewerHeader from './SourceViewerHeader';
import DuplicationPopup from './components/DuplicationPopup';
import {
  filterDuplicationBlocksByLine,
  getDuplicationBlocksForIndex,
  isDuplicationBlockInRemovedComponent,
} from './helpers/duplications';
import getCoverageStatus from './helpers/getCoverageStatus';
import {
  duplicationsByLine,
  issuesByLine,
  locationsByLine,
  symbolsByLine,
} from './helpers/indexing';
import { LINES_TO_LOAD } from './helpers/lines';
import loadIssues from './helpers/loadIssues';
import './styles.css';

export interface Props {
  aroundLine?: number;
  branchLike: BranchLike | undefined;
  component: string;
  componentMeasures?: Measure[];
  displayAllIssues?: boolean;
  displayLocationMarkers?: boolean;
  hideHeader?: boolean;
  hidePinOption?: boolean;
  highlightedLine?: number;
  highlightedLocationMessage?: { index: number; text: string | undefined };
  // `undefined` elements mean they are located in a different file,
  // but kept to maintain the location indexes
  highlightedLocations?: (FlowLocation | undefined)[];
  metricKey?: string;
  needIssueSync?: boolean;
  onIssueSelect?: (issueKey: string) => void;
  onIssueUnselect?: () => void;
  onLoaded?: (component: SourceViewerFile, sources: SourceLine[], issues: Issue[]) => void;
  onLocationSelect?: (index: number) => void;
  selectedIssue?: string;
  showMeasures?: boolean;
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

export class SourceViewerClass extends React.PureComponent<Props, State> {
  mounted = false;

  static defaultProps = {
    displayAllIssues: false,
    displayIssueLocationsCount: true,
    displayIssueLocationsLink: true,
    displayLocationMarkers: true,
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
      symbolsByLine: {},
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchComponent();
  }

  async componentDidUpdate(prevProps: Props) {
    if (
      this.props.onIssueSelect !== undefined &&
      this.props.selectedIssue !== prevProps.selectedIssue
    ) {
      this.setState({ selectedIssue: this.props.selectedIssue });
    }

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
      const sources = await this.fetchSources().catch(() => []);

      if (this.mounted) {
        const finalSources = sources.slice(0, LINES_TO_LOAD);

        this.setState(
          {
            sources: sources.slice(0, LINES_TO_LOAD),
            hasSourcesAfter: sources.length > LINES_TO_LOAD,
          },
          () => {
            if (this.props.onLoaded && this.state.component && this.state.issues) {
              this.props.onLoaded(this.state.component, finalSources, this.state.issues);
            }
          },
        );
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadComponent(component: string, branchLike?: BranchLike) {
    return Promise.all([
      getComponentForSourceViewer({ component, ...getBranchLikeQuery(branchLike) }),
      getComponentData({ component, ...getBranchLikeQuery(branchLike) }),
    ]).then(([sourceViewerComponent, { component }]) => ({
      ...sourceViewerComponent,
      leakPeriodDate: component.leakPeriodDate,
    }));
  }

  loadSources(
    key: string,
    from: number | undefined,
    to: number | undefined,
    branchLike: BranchLike | undefined,
  ) {
    return getSources({ key, from, to, ...getBranchLikeQuery(branchLike) });
  }

  computeCoverageStatus(lines: SourceLine[]) {
    return lines.map((line) => ({ ...line, coverageStatus: getCoverageStatus(line) }));
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

    const loadIssuesCallback = (component: SourceViewerFile, sources: SourceLine[]) => {
      loadIssues(this.props.component, this.props.branchLike, this.props.needIssueSync).then(
        (issues) => {
          if (this.mounted) {
            const finalSources = sources.slice(0, LINES_TO_LOAD);

            this.setState(
              {
                component,
                duplicatedFiles: undefined,
                duplications: undefined,
                duplicationsByLine: {},
                hasSourcesAfter: sources.length > LINES_TO_LOAD,
                highlightedSymbols: [],
                issueLocationsByLine: locationsByLine(issues),
                issuePopup: undefined,
                issues,
                issuesByLine: issuesByLine(issues),
                loading: false,
                notAccessible: false,
                notExist: false,
                openIssuesByLine: {},
                sourceRemoved: false,
                sources: this.computeCoverageStatus(finalSources),
                symbolsByLine: symbolsByLine(sources.slice(0, LINES_TO_LOAD)),
              },
              () => {
                if (this.props.onLoaded) {
                  this.props.onLoaded(component, finalSources, issues);
                }
              },
            );
          }
        },
        () => {
          /* no op */
        },
      );
    };

    const onFailLoadComponent = (response: Response) => {
      if (this.mounted) {
        if (response.status === HttpStatus.Forbidden) {
          this.setState({ loading: false, notAccessible: true });
        } else if (response.status === HttpStatus.NotFound) {
          this.setState({ loading: false, notExist: true });
        }
      }
    };

    const onFailLoadSources = (response: Response, component: SourceViewerFile) => {
      if (this.mounted) {
        if (response.status === HttpStatus.Forbidden) {
          this.setState({ component, loading: false, notAccessible: true });
        } else if (response.status === HttpStatus.NotFound) {
          this.setState({ component, loading: false, sourceRemoved: true });
        }
      }
    };

    const onResolve = (component: SourceViewerFile) => {
      const sourcesRequest =
        component.q === ComponentQualifier.File || component.q === ComponentQualifier.TestFile
          ? this.fetchSources()
          : Promise.resolve([]);

      sourcesRequest.then(
        (sources) => loadIssuesCallback(component, sources),
        (response) => onFailLoadSources(response, component),
      );
    };

    this.loadComponent(this.props.component, this.props.branchLike).then(
      onResolve,
      onFailLoadComponent,
    );
  }

  fetchSources = (): Promise<SourceLine[]> => {
    return new Promise((resolve, reject) => {
      const onFailLoadSources = (response: Response) => {
        if (this.mounted) {
          if ([HttpStatus.Forbidden, HttpStatus.NotFound].includes(response.status)) {
            reject(response);
          } else {
            resolve([]);
          }
        }
      };

      const from = this.props.aroundLine
        ? Math.max(1, this.props.aroundLine - LINES_TO_LOAD / 2 + 1)
        : 1;

      let to = this.props.aroundLine
        ? this.props.aroundLine + LINES_TO_LOAD / 2 + 1
        : LINES_TO_LOAD + 1;

      // make sure we try to download `LINES` lines
      if (from === 1 && to < LINES_TO_LOAD) {
        to = LINES_TO_LOAD;
      }

      // request one additional line to define `hasSourcesAfter`
      to++;

      this.loadSources(this.props.component, from, to, this.props.branchLike).then((sources) => {
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

    const from = Math.max(1, firstSourceLine.line - LINES_TO_LOAD);

    this.loadSources(
      this.props.component,
      from,
      firstSourceLine.line - 1,
      this.props.branchLike,
    ).then(
      (sources) => {
        if (this.mounted) {
          this.setState((prevState) => {
            return {
              loadingSourcesBefore: false,
              sources: [...this.computeCoverageStatus(sources), ...(prevState.sources || [])],
              symbolsByLine: { ...prevState.symbolsByLine, ...symbolsByLine(sources) },
            };
          });
        }
      },
      () => {
        /* no op */
      },
    );
  };

  loadSourcesAfter = () => {
    if (!this.state.sources) {
      return;
    }

    const lastSourceLine = this.state.sources[this.state.sources.length - 1];

    this.setState({ loadingSourcesAfter: true });

    const fromLine = lastSourceLine.line + 1;
    const toLine = lastSourceLine.line + LINES_TO_LOAD + 1;

    this.loadSources(this.props.component, fromLine, toLine, this.props.branchLike).then(
      (sources) => {
        if (this.mounted) {
          const hasSourcesAfter = LINES_TO_LOAD < sources.length;

          if (hasSourcesAfter) {
            sources.pop();
          }

          this.setState((prevState) => {
            return {
              hasSourcesAfter,
              loadingSourcesAfter: false,
              sources: [...(prevState.sources || []), ...this.computeCoverageStatus(sources)],
              symbolsByLine: {
                ...prevState.symbolsByLine,
                ...symbolsByLine(sources),
              },
            };
          });
        }
      },
      () => {
        /* no op */
      },
    );
  };

  loadDuplications = () => {
    getDuplications({
      key: this.props.component,
      ...getBranchLikeQuery(this.props.branchLike),
    }).then(
      (r) => {
        if (this.mounted) {
          this.setState({
            duplications: r.duplications,
            duplicationsByLine: duplicationsByLine(r.duplications),
            duplicatedFiles: r.files,
          });
        }
      },
      () => {
        /* no op */
      },
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
    this.setState((state) => {
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
    this.setState((state) => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: true },
    }));
  };

  handleCloseIssues = (line: SourceLine) => {
    this.setState((state) => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: false },
    }));
  };

  handleIssueChange = (issue: Issue) => {
    this.setState(({ issues = [] }) => {
      const newIssues = issues.map((candidate) =>
        candidate.key === issue.key ? issue : candidate,
      );

      return { issues: newIssues, issuesByLine: issuesByLine(newIssues) };
    });
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
            duplicationHeader={translate('component_viewer.transition.duplication')}
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
        displayAllIssues={this.props.displayAllIssues}
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
        loadingSourcesAfter={this.state.loadingSourcesAfter}
        loadingSourcesBefore={this.state.loadingSourcesBefore}
        loadSourcesAfter={this.loadSourcesAfter}
        loadSourcesBefore={this.loadSourcesBefore}
        metricKey={this.props.metricKey}
        onIssueChange={this.handleIssueChange}
        onIssuePopupToggle={this.handleIssuePopupToggle}
        onIssuesClose={this.handleCloseIssues}
        onIssueSelect={this.handleIssueSelect}
        onIssuesOpen={this.handleOpenIssues}
        onIssueUnselect={this.handleIssueUnselect}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={this.handleSymbolClick}
        openIssuesByLine={this.state.openIssuesByLine}
        renderDuplicationPopup={this.renderDuplicationPopup}
        selectedIssue={this.state.selectedIssue}
        sources={sources}
        symbolsByLine={this.state.symbolsByLine}
      />
    );
  }

  renderHeader(sourceViewerFile: SourceViewerFile) {
    return (
      <WorkspaceContext.Consumer>
        {({ openComponent }) => (
          <SourceViewerHeader
            branchLike={this.props.branchLike}
            componentMeasures={this.props.componentMeasures}
            hidePinOption={this.props.hidePinOption}
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
    const { hideHeader } = this.props;

    if (loading) {
      return null;
    }

    if (this.state.notExist) {
      return (
        <FlagMessage className="sw-mt-2" variant="warning">
          {translate('component_viewer.no_component')}
        </FlagMessage>
      );
    }

    if (notAccessible) {
      return (
        <FlagMessage className="sw-mt-2" variant="warning">
          {translate('code_viewer.no_source_code_displayed_due_to_security')}
        </FlagMessage>
      );
    }

    if (!component) {
      return null;
    }

    return (
      <SourceViewerContext.Provider value={{ branchLike: this.props.branchLike, file: component }}>
        <div className="source-viewer">
          {!hideHeader && this.renderHeader(component)}

          {sourceRemoved && (
            <FlagMessage className="sw-mt-4 sw-ml-4" variant="warning">
              {translate('code_viewer.no_source_code_displayed_due_to_source_removed')}
            </FlagMessage>
          )}

          {!sourceRemoved && sources !== undefined && this.renderCode(sources)}
        </div>
      </SourceViewerContext.Provider>
    );
  }
}

export default function SourceViewer(props: Props) {
  return (
    // we can't use withComponentContext as it would override the "component" prop
    <ComponentContext.Consumer>
      {({ component }) => <SourceViewerClass needIssueSync={component?.needIssueSync} {...props} />}
    </ComponentContext.Consumer>
  );
}
