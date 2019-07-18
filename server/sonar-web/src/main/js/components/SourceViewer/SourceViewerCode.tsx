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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Line from './components/Line';
import { getSecondaryIssueLocationsForLine } from './helpers/issueLocations';
import {
  optimizeHighlightedSymbols,
  optimizeLocationMessage,
  optimizeSelectedIssue
} from './helpers/lines';

const EMPTY_ARRAY: any[] = [];

const ZERO_LINE = {
  code: '',
  duplicated: false,
  isNew: false,
  line: 0
};

interface Props {
  branchLike: T.BranchLike | undefined;
  componentKey: string;
  displayAllIssues?: boolean;
  displayLocationMarkers?: boolean;
  duplications: T.Duplication[] | undefined;
  duplicationsByLine: { [line: number]: number[] };
  hasSourcesAfter: boolean;
  hasSourcesBefore: boolean;
  highlightedLine: number | undefined;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  // `undefined` elements mean they are located in a different file,
  // but kept to maintain the location indexes
  highlightedLocations: (T.FlowLocation | undefined)[] | undefined;
  highlightedSymbols: string[];
  issueLocationsByLine: { [line: number]: T.LinearIssueLocation[] };
  issuePopup: { issue: string; name: string } | undefined;
  issues: T.Issue[] | undefined;
  issuesByLine: { [line: number]: T.Issue[] };
  linePopup: T.LinePopup | undefined;
  loadDuplications: (line: T.SourceLine) => void;
  loadingSourcesAfter: boolean;
  loadingSourcesBefore: boolean;
  loadSourcesAfter: () => void;
  loadSourcesBefore: () => void;
  onIssueChange: (issue: T.Issue) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onIssuesClose: (line: T.SourceLine) => void;
  onIssueSelect: (issueKey: string) => void;
  onIssuesOpen: (line: T.SourceLine) => void;
  onIssueUnselect: () => void;
  onLinePopupToggle: (linePopup: T.LinePopup) => void;
  onLocationSelect: ((index: number) => void) | undefined;
  onSymbolClick: (symbols: string[]) => void;
  openIssuesByLine: { [line: number]: boolean };
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
  scroll?: (element: HTMLElement) => void;
  selectedIssue: string | undefined;
  sources: T.SourceLine[];
  symbolsByLine: { [line: number]: string[] };
}

export default class SourceViewerCode extends React.PureComponent<Props> {
  getDuplicationsForLine = (line: T.SourceLine): number[] => {
    return this.props.duplicationsByLine[line.line] || EMPTY_ARRAY;
  };

  getIssuesForLine = (line: T.SourceLine): T.Issue[] => {
    return this.props.issuesByLine[line.line] || EMPTY_ARRAY;
  };

  getIssueLocationsForLine = (line: T.SourceLine): T.LinearIssueLocation[] => {
    return this.props.issueLocationsByLine[line.line] || EMPTY_ARRAY;
  };

  renderLine = ({
    line,
    index,
    displayCoverage,
    displayDuplications,
    displayIssues
  }: {
    line: T.SourceLine;
    index: number;
    displayCoverage: boolean;
    displayDuplications: boolean;
    displayIssues: boolean;
  }) => {
    const { highlightedLocationMessage, highlightedLocations, selectedIssue, sources } = this.props;

    const secondaryIssueLocations = getSecondaryIssueLocationsForLine(line, highlightedLocations);

    const duplicationsCount = this.props.duplications ? this.props.duplications.length : 0;

    const issuesForLine = this.getIssuesForLine(line);

    return (
      <Line
        branchLike={this.props.branchLike}
        displayAllIssues={this.props.displayAllIssues}
        displayCoverage={displayCoverage}
        displayDuplications={displayDuplications}
        displayIssues={displayIssues}
        displayLocationMarkers={this.props.displayLocationMarkers}
        duplications={this.getDuplicationsForLine(line)}
        duplicationsCount={duplicationsCount}
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
        issuePopup={this.props.issuePopup}
        issues={issuesForLine}
        key={line.line}
        last={index === this.props.sources.length - 1 && !this.props.hasSourcesAfter}
        line={line}
        linePopup={this.props.linePopup}
        loadDuplications={this.props.loadDuplications}
        onIssueChange={this.props.onIssueChange}
        onIssuePopupToggle={this.props.onIssuePopupToggle}
        onIssueSelect={this.props.onIssueSelect}
        onIssueUnselect={this.props.onIssueUnselect}
        onIssuesClose={this.props.onIssuesClose}
        onIssuesOpen={this.props.onIssuesOpen}
        onLinePopupToggle={this.props.onLinePopupToggle}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={this.props.onSymbolClick}
        openIssues={this.props.openIssuesByLine[line.line] || false}
        previousLine={index > 0 ? sources[index - 1] : undefined}
        renderDuplicationPopup={this.props.renderDuplicationPopup}
        scroll={this.props.scroll}
        secondaryIssueLocations={secondaryIssueLocations}
        selectedIssue={optimizeSelectedIssue(selectedIssue, issuesForLine)}
      />
    );
  };

  render() {
    const { issues = [], sources } = this.props;

    const displayCoverage = sources.some(s => s.coverageStatus != null);
    const displayDuplications = sources.some(s => !!s.duplicated);
    const displayIssues = issues.length > 0;

    const hasFileIssues = displayIssues && issues.some(issue => !issue.textRange);

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
                onClick={this.props.loadSourcesBefore}>
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
                displayIssues
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
                onClick={this.props.loadSourcesAfter}>
                {translate('source_viewer.load_more_code')}
              </Button>
            )}
          </div>
        )}
      </div>
    );
  }
}
