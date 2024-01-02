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
import { ButtonSecondary, LightLabel, SonarCodeColorizer, Spinner } from 'design-system';
import * as React from 'react';
import { decorateWithUnderlineFlags } from '../../helpers/code-viewer';
import { translate } from '../../helpers/l10n';
import { BranchLike } from '../../types/branch-like';
import { MetricKey } from '../../types/metrics';
import {
  Duplication,
  FlowLocation,
  Issue,
  LineMap,
  LinearIssueLocation,
  SourceLine,
} from '../../types/types';
import Line from './components/Line';
import LineIssuesList from './components/LineIssuesList';
import { getSecondaryIssueLocationsForLine } from './helpers/issueLocations';
import { optimizeHighlightedSymbols, optimizeLocationMessage } from './helpers/lines';

const EMPTY_ARRAY: unknown[] = [];

const ZERO_LINE = {
  code: '',
  duplicated: false,
  isNew: false,
  line: 0,
};

interface State {
  decoratedLinesMap: LineMap;
  hoveredLine?: SourceLine;
}

interface Props {
  branchLike: BranchLike | undefined;
  displayAllIssues?: boolean;
  displayLocationMarkers?: boolean;
  duplications: Duplication[] | undefined;
  duplicationsByLine: { [line: number]: number[] };
  hasSourcesAfter: boolean;
  hasSourcesBefore: boolean;
  highlightedLine: number | undefined;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  // `undefined` elements mean they are located in a different file,
  // but kept to maintain the location indexes
  highlightedLocations: (FlowLocation | undefined)[] | undefined;
  highlightedSymbols: string[];
  issueLocationsByLine: { [line: number]: LinearIssueLocation[] };
  issuePopup: { issue: string; name: string } | undefined;
  issues: Issue[] | undefined;
  issuesByLine: { [line: number]: Issue[] };
  loadDuplications: (line: SourceLine) => void;
  loadingSourcesAfter: boolean;
  loadingSourcesBefore: boolean;
  loadSourcesAfter: () => void;
  loadSourcesBefore: () => void;
  metricKey?: string;
  onIssueChange: (issue: Issue) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onIssuesClose: (line: SourceLine) => void;
  onIssueSelect: (issueKey: string) => void;
  onIssuesOpen: (line: SourceLine) => void;
  onIssueUnselect: () => void;
  onLocationSelect: ((index: number) => void) | undefined;
  onSymbolClick: (symbols: string[]) => void;
  openIssuesByLine: { [line: number]: boolean };
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
  selectedIssue: string | undefined;
  sources: SourceLine[];
  symbolsByLine: { [line: number]: string[] };
}

export default class SourceViewerCode extends React.PureComponent<Props, State> {
  firstUncoveredLineFound = false;

  constructor(props: Props) {
    super(props);

    this.state = {
      decoratedLinesMap: this.getDecoratedLinesMap(props.sources),
      hoveredLine: undefined,
    };
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.metricKey !== prevProps.metricKey) {
      this.firstUncoveredLineFound = false;
    }

    if (this.props.sources !== prevProps.sources) {
      this.setState({
        decoratedLinesMap: this.getDecoratedLinesMap(this.props.sources),
      });
    }
  }

  getDecoratedLinesMap = (sources: SourceLine[]) =>
    sources.reduce((map: LineMap, line: SourceLine) => {
      map[line.line] = decorateWithUnderlineFlags(line, map);

      return map;
    }, {});

  getDuplicationsForLine = (line: SourceLine): number[] => {
    return this.props.duplicationsByLine[line.line] || EMPTY_ARRAY;
  };

  getIssuesForLine = (line: SourceLine): Issue[] => {
    return this.props.issuesByLine[line.line] || EMPTY_ARRAY;
  };

  getIssueLocationsForLine = (line: SourceLine): LinearIssueLocation[] => {
    return this.props.issueLocationsByLine[line.line] || EMPTY_ARRAY;
  };

  onLineMouseEnter = (hoveredLineNumber: number) =>
    this.setState(({ decoratedLinesMap }) => ({
      hoveredLine: decoratedLinesMap[hoveredLineNumber],
    }));

  onLineMouseLeave = (leftLineNumber: number) =>
    this.setState(({ hoveredLine }) => ({
      hoveredLine: hoveredLine?.line === leftLineNumber ? undefined : hoveredLine,
    }));

  renderLine = ({
    displayCoverage,
    displayDuplications,
    displayIssues,
    index,
    line,
  }: {
    displayCoverage: boolean;
    displayDuplications: boolean;
    displayIssues: boolean;
    index: number;
    line: SourceLine;
  }) => {
    const { hoveredLine } = this.state;

    const {
      branchLike,
      displayAllIssues,
      displayLocationMarkers,
      duplications,
      highlightedLine,
      highlightedLocationMessage,
      highlightedLocations,
      highlightedSymbols,
      issueLocationsByLine,
      issuePopup,
      metricKey,
      openIssuesByLine,
      selectedIssue,
      sources,
      symbolsByLine,
    } = this.props;

    const secondaryIssueLocations = getSecondaryIssueLocationsForLine(line, highlightedLocations);

    const duplicationsCount = duplications?.length ?? 0;

    const issuesForLine = this.getIssuesForLine(line);

    const firstLineNumber = sources?.length ? sources[0].line : 0;

    let scrollToUncoveredLine = false;

    if (
      !this.firstUncoveredLineFound &&
      displayCoverage &&
      line.coverageStatus &&
      ['uncovered', 'partially-covered'].includes(line.coverageStatus)
    ) {
      scrollToUncoveredLine =
        (metricKey === MetricKey.new_uncovered_lines && line.isNew) ||
        metricKey === MetricKey.uncovered_lines;

      this.firstUncoveredLineFound = scrollToUncoveredLine;
    }

    const displayCoverageUnderline = !!(
      hoveredLine?.coverageBlock && hoveredLine.coverageBlock === line.coverageBlock
    );

    return (
      <Line
        displayAllIssues={displayAllIssues}
        displayCoverage={displayCoverage}
        displayCoverageUnderline={displayCoverageUnderline}
        displayDuplications={displayDuplications}
        displayIssues={displayIssues}
        displayLocationMarkers={displayLocationMarkers}
        displayNewCodeUnderline={hoveredLine?.newCodeBlock === line.line}
        displaySCM={sources.length > 0}
        duplications={this.getDuplicationsForLine(line)}
        duplicationsCount={duplicationsCount}
        firstLineNumber={firstLineNumber}
        highlighted={line.line === highlightedLine}
        highlightedLocationMessage={optimizeLocationMessage(
          highlightedLocationMessage,
          secondaryIssueLocations,
        )}
        highlightedSymbols={optimizeHighlightedSymbols(
          symbolsByLine[line.line],
          highlightedSymbols,
        )}
        issueLocations={this.getIssueLocationsForLine(line)}
        issues={issuesForLine}
        key={line.line || line.code}
        line={line}
        loadDuplications={this.props.loadDuplications}
        onIssuesClose={this.props.onIssuesClose}
        onIssueSelect={this.props.onIssueSelect}
        onIssuesOpen={this.props.onIssuesOpen}
        onIssueUnselect={this.props.onIssueUnselect}
        onLineMouseEnter={this.onLineMouseEnter}
        onLineMouseLeave={this.onLineMouseLeave}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={this.props.onSymbolClick}
        openIssues={openIssuesByLine[line.line] || false}
        previousLine={index > 0 ? sources[index - 1] : undefined}
        renderDuplicationPopup={this.props.renderDuplicationPopup}
        scrollToUncoveredLine={scrollToUncoveredLine}
        secondaryIssueLocations={secondaryIssueLocations}
      >
        <LineIssuesList
          branchLike={branchLike}
          displayAllIssues={displayAllIssues}
          displayWhyIsThisAnIssue
          issueLocationsByLine={issueLocationsByLine}
          issuePopup={issuePopup}
          issuesForLine={issuesForLine}
          line={line}
          onIssueChange={this.props.onIssueChange}
          onIssueClick={this.props.onIssueSelect}
          onIssuePopupToggle={this.props.onIssuePopupToggle}
          openIssuesByLine={openIssuesByLine}
          selectedIssue={selectedIssue}
        />
      </Line>
    );
  };

  render() {
    const { decoratedLinesMap } = this.state;

    const {
      hasSourcesAfter,
      hasSourcesBefore,
      issues = [],
      loadingSourcesAfter,
      loadingSourcesBefore,
      sources,
    } = this.props;

    const displayCoverage = sources.some((s) => s.coverageStatus != null);
    const displayDuplications = sources.some((s) => !!s.duplicated);
    const displayIssues = issues.length > 0;

    const hasFileIssues = displayIssues && issues.some((issue) => !issue.textRange);

    return (
      <SonarCodeColorizer>
        <div className="it__source-viewer-code">
          {hasSourcesBefore && (
            <div className="sw-flex sw-justify-center sw-p-6">
              {loadingSourcesBefore ? (
                <div className="sw-flex sw-items-center">
                  <Spinner loading />
                  <LightLabel className="sw-ml-2">
                    {translate('source_viewer.loading_more_code')}
                  </LightLabel>
                </div>
              ) : (
                <ButtonSecondary onClick={this.props.loadSourcesBefore}>
                  {translate('source_viewer.load_more_code')}
                </ButtonSecondary>
              )}
            </div>
          )}

          <table className="source-table">
            <tbody>
              {hasFileIssues &&
                this.renderLine({
                  displayCoverage,
                  displayDuplications,
                  displayIssues,
                  index: -1,
                  line: ZERO_LINE,
                })}
              {sources.map((line, index) =>
                this.renderLine({
                  displayCoverage,
                  displayDuplications,
                  displayIssues,
                  index,
                  line: decoratedLinesMap[line.line] || line,
                }),
              )}
            </tbody>
          </table>

          {hasSourcesAfter && (
            <div className="sw-flex sw-justify-center sw-p-6">
              {loadingSourcesAfter ? (
                <div className="sw-flex sw-items-center">
                  <Spinner loading />
                  <LightLabel className="sw-ml-2">
                    {translate('source_viewer.loading_more_code')}
                  </LightLabel>
                </div>
              ) : (
                <ButtonSecondary onClick={this.props.loadSourcesAfter}>
                  {translate('source_viewer.load_more_code')}
                </ButtonSecondary>
              )}
            </div>
          )}
        </div>
      </SonarCodeColorizer>
    );
  }
}
