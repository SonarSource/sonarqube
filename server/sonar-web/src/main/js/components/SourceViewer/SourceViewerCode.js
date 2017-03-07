/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import SourceViewerLine from './SourceViewerLine';
import { TooltipsContainer } from '../mixins/tooltips-mixin';
import { translate } from '../../helpers/l10n';
import type { Duplication, SourceLine } from './types';
import type { Issue } from '../issue/types';
import type {
  LinearIssueLocation,
  IndexedIssueLocation,
  IndexedIssueLocationsByIssueAndLine,
  IndexedIssueLocationMessagesByIssueAndLine
} from './helpers/indexing';

const EMPTY_ARRAY = [];

const ZERO_LINE = {
  code: '',
  duplicated: false,
  line: 0
};

export default class SourceViewerCode extends React.PureComponent {
  props: {
    displayAllIssues: boolean,
    duplications?: Array<Duplication>,
    duplicationsByLine: { [number]: Array<number> },
    duplicatedFiles?: Array<{ key: string }>,
    filterLine?: (SourceLine) => boolean,
    hasSourcesAfter: boolean,
    hasSourcesBefore: boolean,
    highlightedLine: number | null,
    highlightedSymbol: string | null,
    issues: Array<Issue>,
    issuesByLine: { [number]: Array<string> },
    issueLocationsByLine: { [number]: Array<LinearIssueLocation> },
    issueSecondaryLocationsByIssueByLine: IndexedIssueLocationsByIssueAndLine,
    issueSecondaryLocationMessagesByIssueByLine: IndexedIssueLocationMessagesByIssueAndLine,
    loadDuplications: (SourceLine, HTMLElement) => void,
    loadSourcesAfter: () => void,
    loadSourcesBefore: () => void,
    loadingSourcesAfter: boolean,
    loadingSourcesBefore: boolean,
    onCoverageClick: (SourceLine, HTMLElement) => void,
    onDuplicationClick: (number, number) => void,
    onIssueSelect: (string) => void,
    onIssueUnselect: () => void,
    onLineClick: (number, HTMLElement) => void,
    onSCMClick: (SourceLine, HTMLElement) => void,
    onSelectLocation: (flowIndex: number, locationIndex: number) => void,
    onSymbolClick: (string) => void,
    selectedIssue: string | null,
    selectedIssueLocation: IndexedIssueLocation | null,
    sources: Array<SourceLine>,
    symbolsByLine: { [number]: Array<string> }
  };

  isSCMChanged (s: SourceLine, p: null | SourceLine) {
    let changed = true;
    if (p != null && s.scmAuthor != null && p.scmAuthor != null) {
      changed = (s.scmAuthor !== p.scmAuthor) || (s.scmDate !== p.scmDate);
    }
    return changed;
  }

  getDuplicationsForLine (line: SourceLine) {
    return this.props.duplicationsByLine[line.line] || EMPTY_ARRAY;
  }

  getIssuesForLine (line: SourceLine): Array<string> {
    return this.props.issuesByLine[line.line] || EMPTY_ARRAY;
  }

  getIssueLocationsForLine (line: SourceLine) {
    return this.props.issueLocationsByLine[line.line] || EMPTY_ARRAY;
  }

  getSecondaryIssueLocationsForLine (line: SourceLine, issueKey: string) {
    const index = this.props.issueSecondaryLocationsByIssueByLine;
    if (index[issueKey] == null) {
      return EMPTY_ARRAY;
    }
    return index[issueKey][line.line] || EMPTY_ARRAY;
  }

  getSecondaryIssueLocationMessagesForLine (line: SourceLine, issueKey: string) {
    return this.props.issueSecondaryLocationMessagesByIssueByLine[issueKey][line.line] || EMPTY_ARRAY;
  }

  renderLine = (
    line: SourceLine,
    index: number,
    displayCoverage: boolean,
    displayDuplications: boolean,
    displayFiltered: boolean,
    displayIssues: boolean
  ) => {
    const { filterLine, selectedIssue, sources } = this.props;
    const filtered = filterLine ? filterLine(line) : null;
    const secondaryIssueLocations = selectedIssue ?
      this.getSecondaryIssueLocationsForLine(line, selectedIssue) : EMPTY_ARRAY;
    const secondaryIssueLocationMessages = selectedIssue ?
      this.getSecondaryIssueLocationMessagesForLine(line, selectedIssue) : EMPTY_ARRAY;

    const duplicationsCount = this.props.duplications ? this.props.duplications.length : 0;

    const issuesForLine = this.getIssuesForLine(line);

    // for the following properties pass null if the line for sure is not impacted
    const symbolsForLine = this.props.symbolsByLine[line.line] || [];
    const { highlightedSymbol } = this.props;
    const optimizedHighlightedSymbol = highlightedSymbol != null && symbolsForLine.includes(highlightedSymbol) ?
      highlightedSymbol : null;

    const optimizedSelectedIssue = selectedIssue != null && issuesForLine.includes(selectedIssue) ?
      selectedIssue : null;

    const { selectedIssueLocation } = this.props;
    const optimizedSelectedIssueLocation =
      selectedIssueLocation != null &&
        secondaryIssueLocations.some(location =>
          location.flowIndex === selectedIssueLocation.flowIndex &&
          location.locationIndex === selectedIssueLocation.locationIndex
        ) ? selectedIssueLocation : null;

    return (
      <SourceViewerLine
        displayAllIssues={this.props.displayAllIssues}
        displayCoverage={displayCoverage}
        displayDuplications={displayDuplications}
        displayFiltered={displayFiltered}
        displayIssues={displayIssues}
        displaySCM={this.isSCMChanged(line, index > 0 ? sources[index - 1] : null)}
        duplications={this.getDuplicationsForLine(line)}
        duplicationsCount={duplicationsCount}
        filtered={filtered}
        highlighted={line.line === this.props.highlightedLine}
        highlightedSymbol={optimizedHighlightedSymbol}
        issueLocations={this.getIssueLocationsForLine(line)}
        issues={issuesForLine}
        key={line.line}
        line={line}
        loadDuplications={this.props.loadDuplications}
        onClick={this.props.onLineClick}
        onCoverageClick={this.props.onCoverageClick}
        onDuplicationClick={this.props.onDuplicationClick}
        onIssueSelect={this.props.onIssueSelect}
        onIssueUnselect={this.props.onIssueUnselect}
        onSCMClick={this.props.onSCMClick}
        onSelectLocation={this.props.onSelectLocation}
        onSymbolClick={this.props.onSymbolClick}
        secondaryIssueLocations={secondaryIssueLocations}
        secondaryIssueLocationMessages={secondaryIssueLocationMessages}
        selectedIssue={optimizedSelectedIssue}
        selectedIssueLocation={optimizedSelectedIssueLocation}/>
    );
  };

  render () {
    const { sources } = this.props;

    const hasCoverage = sources.some(s => s.coverageStatus != null);
    const hasDuplications = sources.some(s => s.duplicated);
    const displayFiltered = this.props.filterLine != null;
    const hasIssues = this.props.issues.length > 0;

    const hasFileIssues = hasIssues && this.props.issues.some(issue => !issue.line);

    return (
      <div>
        {this.props.hasSourcesBefore && (
          <div className="source-viewer-more-code">
            {this.props.loadingSourcesBefore ? (
                <div className="js-component-viewer-loading-before">
                  <i className="spinner"/>
                  <span className="note spacer-left">{translate('source_viewer.loading_more_code')}</span>
                </div>
              ) : (
                <button className="js-component-viewer-source-before" onClick={this.props.loadSourcesBefore}>
                  {translate('source_viewer.load_more_code')}
                </button>
              )}
          </div>
        )}

        <TooltipsContainer>
          <table className="source-table">
            <tbody>
              {hasFileIssues && (
                this.renderLine(ZERO_LINE, -1, hasCoverage, hasDuplications, displayFiltered, hasIssues)
              )}
              {sources.map((line, index) => (
                this.renderLine(line, index, hasCoverage, hasDuplications, displayFiltered, hasIssues)
              ))}
            </tbody>
          </table>
        </TooltipsContainer>

        {this.props.hasSourcesAfter && (
          <div className="source-viewer-more-code">
            {this.props.loadingSourcesAfter ? (
                <div className="js-component-viewer-loading-after">
                  <i className="spinner"/>
                  <span className="note spacer-left">{translate('source_viewer.loading_more_code')}</span>
                </div>
              ) : (
                <button className="js-component-viewer-source-after" onClick={this.props.loadSourcesAfter}>
                  {translate('source_viewer.load_more_code')}
                </button>
              )}
          </div>
        )}
      </div>
    );
  }
}
