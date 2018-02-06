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
import { intersection } from 'lodash';
import Line from './components/Line';
import { getLinearLocations } from './helpers/issueLocations';
import { translate } from '../../helpers/l10n';
/*:: import type { Duplication, SourceLine } from './types'; */
/*:: import type { Issue, FlowLocation } from '../issue/types'; */
/*:: import type { LinearIssueLocation } from './helpers/indexing'; */

const EMPTY_ARRAY = [];

const ZERO_LINE = {
  code: '',
  duplicated: false,
  line: 0
};

export default class SourceViewerCode extends React.PureComponent {
  /*:: props: {|
    branch?: string,
    displayAllIssues: boolean,
    displayIssueLocationsCount?: boolean;
    displayIssueLocationsLink?: boolean;
    displayLocationMarkers?: boolean;
    duplications?: Array<Duplication>,
    duplicationsByLine: { [number]: Array<number> },
    duplicatedFiles?: Array<{ key: string }>,
    filterLine?: SourceLine => boolean,
    hasSourcesAfter: boolean,
    hasSourcesBefore: boolean,
    highlightedLine: number | null,
    highlightedLocations?: Array<FlowLocation>,
    highlightedLocationMessage?: { index: number, text: string },
    highlightedSymbols: Array<string>,
    issues: Array<Issue>,
    issuesByLine: { [number]: Array<Issue> },
    issueLocationsByLine: { [number]: Array<LinearIssueLocation> },
    loadDuplications: SourceLine => void,
    loadSourcesAfter: () => void,
    loadSourcesBefore: () => void,
    loadingSourcesAfter: boolean,
    loadingSourcesBefore: boolean,
    onCoverageClick: (SourceLine, HTMLElement) => void,
    onDuplicationClick: (number, number) => void,
    onIssueChange: Issue => void,
    onIssueSelect: string => void,
    onIssueUnselect: () => void,
    onIssuesOpen: SourceLine => void,
    onIssuesClose: SourceLine => void,
    onLineClick: (SourceLine, HTMLElement) => void,
    onLocationSelect?: number => void,
    onSCMClick: (SourceLine, HTMLElement) => void,
    onSymbolClick: (Array<string>) => void,
    openIssuesByLine: { [number]: boolean },
    onPopupToggle: (issue: string, popupName: string, open: ?boolean ) => void,
    openPopup: ?{ issue: string, name: string},
    scroll?: HTMLElement => void,
    selectedIssue: string | null,
    sources: Array<SourceLine>,
    symbolsByLine: { [number]: Array<string> }
  |};
*/

  getDuplicationsForLine(line /*: SourceLine */) {
    return this.props.duplicationsByLine[line.line] || EMPTY_ARRAY;
  }

  getIssuesForLine(line /*: SourceLine */) /*: Array<Issue> */ {
    return this.props.issuesByLine[line.line] || EMPTY_ARRAY;
  }

  getIssueLocationsForLine(line /*: SourceLine */) {
    return this.props.issueLocationsByLine[line.line] || EMPTY_ARRAY;
  }

  getSecondaryIssueLocationsForLine(
    line /*: SourceLine */
  ) /*: Array<{ from: number, to: number, line: number, index: number, startLine: number }> */ {
    const { highlightedLocations } = this.props;
    if (!highlightedLocations) {
      return EMPTY_ARRAY;
    }
    return highlightedLocations.reduce((locations, location, index) => {
      const linearLocations = getLinearLocations(location.textRange)
        .filter(l => l.line === line.line)
        .map(l => ({ ...l, startLine: location.textRange.startLine, index }));
      return [...locations, ...linearLocations];
    }, []);
  }

  renderLine = (
    line /*: SourceLine */,
    index /*: number */,
    displayCoverage /*: boolean */,
    displayDuplications /*: boolean */,
    displayIssues /*: boolean */
  ) => {
    const { filterLine, highlightedLocationMessage, selectedIssue, sources } = this.props;
    const filtered = filterLine ? filterLine(line) : null;

    const secondaryIssueLocations = this.getSecondaryIssueLocationsForLine(line);

    const duplicationsCount = this.props.duplications ? this.props.duplications.length : 0;

    const issuesForLine = this.getIssuesForLine(line);

    // for the following properties pass null if the line for sure is not impacted
    const symbolsForLine = this.props.symbolsByLine[line.line] || [];
    const { highlightedSymbols } = this.props;
    let optimizedHighlightedSymbols = intersection(symbolsForLine, highlightedSymbols);
    if (!optimizedHighlightedSymbols.length) {
      optimizedHighlightedSymbols = undefined;
    }

    const optimizedSelectedIssue =
      selectedIssue != null && issuesForLine.find(issue => issue.key === selectedIssue)
        ? selectedIssue
        : null;

    const optimizedSecondaryIssueLocations =
      secondaryIssueLocations.length > 0 ? secondaryIssueLocations : EMPTY_ARRAY;

    const optimizedLocationMessage =
      highlightedLocationMessage != null &&
      optimizedSecondaryIssueLocations.some(
        location => location.index === highlightedLocationMessage.index
      )
        ? highlightedLocationMessage
        : undefined;

    return (
      <Line
        branch={this.props.branch}
        displayAllIssues={this.props.displayAllIssues}
        displayCoverage={displayCoverage}
        displayDuplications={displayDuplications}
        displayIssues={displayIssues}
        displayIssueLocationsCount={this.props.displayIssueLocationsCount}
        displayIssueLocationsLink={this.props.displayIssueLocationsLink}
        displayLocationMarkers={this.props.displayLocationMarkers}
        duplications={this.getDuplicationsForLine(line)}
        duplicationsCount={duplicationsCount}
        filtered={filtered}
        highlighted={line.line === this.props.highlightedLine}
        highlightedLocationMessage={optimizedLocationMessage}
        highlightedSymbols={optimizedHighlightedSymbols}
        issueLocations={this.getIssueLocationsForLine(line)}
        issues={issuesForLine}
        key={line.line}
        last={index === this.props.sources.length - 1 && !this.props.hasSourcesAfter}
        line={line}
        loadDuplications={this.props.loadDuplications}
        onClick={this.props.onLineClick}
        onCoverageClick={this.props.onCoverageClick}
        onDuplicationClick={this.props.onDuplicationClick}
        onIssueChange={this.props.onIssueChange}
        onIssueSelect={this.props.onIssueSelect}
        onIssueUnselect={this.props.onIssueUnselect}
        onIssuesOpen={this.props.onIssuesOpen}
        onIssuesClose={this.props.onIssuesClose}
        onLocationSelect={this.props.onLocationSelect}
        onSCMClick={this.props.onSCMClick}
        onSymbolClick={this.props.onSymbolClick}
        openIssues={this.props.openIssuesByLine[line.line] || false}
        onPopupToggle={this.props.onPopupToggle}
        openPopup={this.props.openPopup}
        previousLine={index > 0 ? sources[index - 1] : undefined}
        scroll={this.props.scroll}
        secondaryIssueLocations={optimizedSecondaryIssueLocations}
        selectedIssue={optimizedSelectedIssue}
      />
    );
  };

  render() {
    const { sources } = this.props;

    const hasCoverage = sources.some(s => s.coverageStatus != null);
    const hasDuplications = sources.some(s => s.duplicated);
    const hasIssues = this.props.issues.length > 0;

    const hasFileIssues = hasIssues && this.props.issues.some(issue => !issue.textRange);

    return (
      <div>
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
              <button
                className="js-component-viewer-source-before"
                onClick={this.props.loadSourcesBefore}>
                {translate('source_viewer.load_more_code')}
              </button>
            )}
          </div>
        )}

        <table className="source-table">
          <tbody>
            {hasFileIssues &&
              this.renderLine(ZERO_LINE, -1, hasCoverage, hasDuplications, hasIssues)}
            {sources.map((line, index) =>
              this.renderLine(line, index, hasCoverage, hasDuplications, hasIssues)
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
              <button
                className="js-component-viewer-source-after"
                onClick={this.props.loadSourcesAfter}>
                {translate('source_viewer.load_more_code')}
              </button>
            )}
          </div>
        )}
      </div>
    );
  }
}
