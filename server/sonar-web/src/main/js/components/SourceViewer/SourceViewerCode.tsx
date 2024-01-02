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
import * as React from 'react';
import { Button } from '../../components/controls/buttons';
import { translate } from '../../helpers/l10n';
import { BranchLike } from '../../types/branch-like';
import { MetricKey } from '../../types/metrics';
import {
  Duplication,
  FlowLocation,
  Issue,
  LinearIssueLocation,
  SourceLine,
} from '../../types/types';
import Line from './components/Line';
import LineIssuesList from './components/LineIssuesList';
import { getSecondaryIssueLocationsForLine } from './helpers/issueLocations';
import { optimizeHighlightedSymbols, optimizeLocationMessage } from './helpers/lines';

const EMPTY_ARRAY: any[] = [];

const ZERO_LINE = {
  code: '',
  duplicated: false,
  isNew: false,
  line: 0,
};

interface Props {
  branchLike: BranchLike | undefined;
  displayAllIssues?: boolean;
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
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
  metricKey?: string;
  selectedIssue: string | undefined;
  sources: SourceLine[];
  symbolsByLine: { [line: number]: string[] };
}

export default class SourceViewerCode extends React.PureComponent<Props> {
  firstUncoveredLineFound = false;

  componentDidUpdate(prevProps: Props) {
    if (this.props.metricKey !== prevProps.metricKey) {
      this.firstUncoveredLineFound = false;
    }
  }

  getDuplicationsForLine = (line: SourceLine): number[] => {
    return this.props.duplicationsByLine[line.line] || EMPTY_ARRAY;
  };

  getIssuesForLine = (line: SourceLine): Issue[] => {
    return this.props.issuesByLine[line.line] || EMPTY_ARRAY;
  };

  getIssueLocationsForLine = (line: SourceLine): LinearIssueLocation[] => {
    return this.props.issueLocationsByLine[line.line] || EMPTY_ARRAY;
  };

  renderLine = ({
    line,
    index,
    displayCoverage,
    displayDuplications,
    displayIssues,
  }: {
    line: SourceLine;
    index: number;
    displayCoverage: boolean;
    displayDuplications: boolean;
    displayIssues: boolean;
  }) => {
    const {
      highlightedLocationMessage,
      selectedIssue,
      openIssuesByLine,
      issueLocationsByLine,
      displayAllIssues,
      highlightedLocations,
      metricKey,
      sources,
    } = this.props;

    const secondaryIssueLocations = getSecondaryIssueLocationsForLine(line, highlightedLocations);

    const duplicationsCount = this.props.duplications ? this.props.duplications.length : 0;

    const issuesForLine = this.getIssuesForLine(line);
    const firstLineNumber = sources && sources.length ? sources[0].line : 0;

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

    return (
      <Line
        displayAllIssues={this.props.displayAllIssues}
        displayCoverage={displayCoverage}
        displayDuplications={displayDuplications}
        displayIssues={displayIssues}
        displayLocationMarkers={this.props.displayLocationMarkers}
        displaySCM={sources.length > 0}
        duplications={this.getDuplicationsForLine(line)}
        duplicationsCount={duplicationsCount}
        firstLineNumber={firstLineNumber}
        highlighted={line.line === this.props.highlightedLine}
        highlightedLocationMessage={optimizeLocationMessage(
          highlightedLocationMessage,
          secondaryIssueLocations
        )}
        highlightedSymbols={optimizeHighlightedSymbols(
          this.props.symbolsByLine[line.line],
          this.props.highlightedSymbols
        )}
        issueLocations={this.getIssueLocationsForLine(line)}
        issues={issuesForLine}
        key={line.line || line.code}
        last={index === this.props.sources.length - 1 && !this.props.hasSourcesAfter}
        line={line}
        loadDuplications={this.props.loadDuplications}
        onIssueSelect={this.props.onIssueSelect}
        onIssueUnselect={this.props.onIssueUnselect}
        onIssuesClose={this.props.onIssuesClose}
        onIssuesOpen={this.props.onIssuesOpen}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={this.props.onSymbolClick}
        openIssues={this.props.openIssuesByLine[line.line] || false}
        previousLine={index > 0 ? sources[index - 1] : undefined}
        renderDuplicationPopup={this.props.renderDuplicationPopup}
        scrollToUncoveredLine={scrollToUncoveredLine}
        secondaryIssueLocations={secondaryIssueLocations}
      >
        <LineIssuesList
          displayWhyIsThisAnIssue={true}
          displayAllIssues={displayAllIssues}
          issueLocationsByLine={issueLocationsByLine}
          issuesForLine={issuesForLine}
          line={line}
          openIssuesByLine={openIssuesByLine}
          branchLike={this.props.branchLike}
          displayIssueLocationsCount={this.props.displayIssueLocationsCount}
          displayIssueLocationsLink={this.props.displayIssueLocationsLink}
          issuePopup={this.props.issuePopup}
          onIssueChange={this.props.onIssueChange}
          onIssueClick={this.props.onIssueSelect}
          onIssuePopupToggle={this.props.onIssuePopupToggle}
          selectedIssue={selectedIssue}
        />
      </Line>
    );
  };

  render() {
    const { issues = [], sources } = this.props;

    const displayCoverage = sources.some((s) => s.coverageStatus != null);
    const displayDuplications = sources.some((s) => !!s.duplicated);
    const displayIssues = issues.length > 0;

    const hasFileIssues = displayIssues && issues.some((issue) => !issue.textRange);

    return (
      <div className="source-viewer-code">
        {this.props.hasSourcesBefore && (
          <div className="source-viewer-more-code">
            {this.props.loadingSourcesBefore ? (
              <div className="js-component-viewer-loading-before">
                <i className="spinner" />
                <span className="note spacer-left">
                  {translate('source_viewer.loading_more_code')}
                </span>
              </div>
            ) : (
              <Button
                className="js-component-viewer-source-before"
                onClick={this.props.loadSourcesBefore}
              >
                {translate('source_viewer.load_more_code')}
              </Button>
            )}
          </div>
        )}

        <table className="source-table">
          <tbody>
            {hasFileIssues &&
              this.renderLine({
                line: ZERO_LINE,
                index: -1,
                displayCoverage,
                displayDuplications,
                displayIssues,
              })}
            {sources.map((line, index) =>
              this.renderLine({ line, index, displayCoverage, displayDuplications, displayIssues })
            )}
          </tbody>
        </table>

        {this.props.hasSourcesAfter && (
          <div className="source-viewer-more-code">
            {this.props.loadingSourcesAfter ? (
              <div className="js-component-viewer-loading-after">
                <i className="spinner" />
                <span className="note spacer-left">
                  {translate('source_viewer.loading_more_code')}
                </span>
              </div>
            ) : (
              <Button
                className="js-component-viewer-source-after"
                onClick={this.props.loadSourcesAfter}
              >
                {translate('source_viewer.load_more_code')}
              </Button>
            )}
          </div>
        )}
      </div>
    );
  }
}
