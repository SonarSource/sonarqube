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
import classNames from 'classnames';
import ExpandSnippetIcon from 'sonar-ui-common/components/icons/ExpandSnippetIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { scrollHorizontally } from 'sonar-ui-common/helpers/scrolling';
import Line from '../../../components/SourceViewer/components/Line';
import { symbolsByLine } from '../../../components/SourceViewer/helpers/indexing';
import { getSecondaryIssueLocationsForLine } from '../../../components/SourceViewer/helpers/issueLocations';
import {
  optimizeHighlightedSymbols,
  optimizeLocationMessage,
  optimizeSelectedIssue
} from '../../../components/SourceViewer/helpers/lines';
import { inSnippet, LINES_BELOW_LAST } from './utils';

interface Props {
  branchLike: T.BranchLike | undefined;
  component: T.SourceViewerFile;
  duplications?: T.Duplication[];
  duplicationsByLine?: { [line: number]: number[] };
  expandBlock: (snippetIndex: number, direction: T.ExpandDirection) => void;
  handleCloseIssues: (line: T.SourceLine) => void;
  handleLinePopupToggle: (line: T.SourceLine) => void;
  handleOpenIssues: (line: T.SourceLine) => void;
  handleSymbolClick: (symbols: string[]) => void;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  highlightedSymbols: string[];
  index: number;
  issue: T.Issue;
  issuePopup?: { issue: string; name: string };
  issuesByLine: T.IssuesByLine;
  last: boolean;
  linePopup?: T.LinePopup;
  loadDuplications: (line: T.SourceLine) => void;
  locations: T.FlowLocation[];
  locationsByLine: { [line: number]: T.LinearIssueLocation[] };
  onIssueChange: (issue: T.Issue) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onLocationSelect: (index: number) => void;
  openIssuesByLine: T.Dict<boolean>;
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
  scroll?: (element: HTMLElement) => void;
  snippet: T.SourceLine[];
}

const SCROLL_LEFT_OFFSET = 32;

export default class SnippetViewer extends React.PureComponent<Props> {
  node: React.RefObject<HTMLDivElement>;

  constructor(props: Props) {
    super(props);
    this.node = React.createRef();
  }

  doScroll = (element: HTMLElement) => {
    if (this.props.scroll) {
      this.props.scroll(element);
    }
    const parent = this.node.current as Element;

    if (parent) {
      scrollHorizontally(element, {
        leftOffset: SCROLL_LEFT_OFFSET,
        rightOffset: parent.getBoundingClientRect().width - SCROLL_LEFT_OFFSET,
        parent
      });
    }
  };

  expandBlock = (direction: T.ExpandDirection) => () =>
    this.props.expandBlock(this.props.index, direction);

  renderLine({
    displayDuplications,
    index,
    issuesForLine,
    issueLocations,
    line,
    snippet,
    symbols,
    verticalBuffer
  }: {
    displayDuplications: boolean;
    index: number;
    issuesForLine: T.Issue[];
    issueLocations: T.LinearIssueLocation[];
    line: T.SourceLine;
    snippet: T.SourceLine[];
    symbols: string[];
    verticalBuffer: number;
  }) {
    const secondaryIssueLocations = getSecondaryIssueLocationsForLine(line, this.props.locations);

    const { duplications, duplicationsByLine } = this.props;
    const duplicationsCount = duplications ? duplications.length : 0;
    const lineDuplications =
      (duplicationsCount && duplicationsByLine && duplicationsByLine[line.line]) || [];

    const isSinkLine = issuesForLine.some(i => i.key === this.props.issue.key);

    return (
      <Line
        branchLike={this.props.branchLike}
        displayAllIssues={false}
        displayCoverage={true}
        displayDuplications={displayDuplications}
        displayIssues={!isSinkLine || issuesForLine.length > 1}
        displayLocationMarkers={true}
        duplications={lineDuplications}
        duplicationsCount={duplicationsCount}
        highlighted={false}
        highlightedLocationMessage={optimizeLocationMessage(
          this.props.highlightedLocationMessage,
          secondaryIssueLocations
        )}
        highlightedSymbols={optimizeHighlightedSymbols(symbols, this.props.highlightedSymbols)}
        issueLocations={issueLocations}
        issuePopup={this.props.issuePopup}
        issues={issuesForLine}
        key={line.line}
        last={false}
        line={line}
        linePopup={this.props.linePopup}
        loadDuplications={this.props.loadDuplications}
        onIssueChange={this.props.onIssueChange}
        onIssuePopupToggle={this.props.onIssuePopupToggle}
        onIssueSelect={() => {}}
        onIssueUnselect={() => {}}
        onIssuesClose={this.props.handleCloseIssues}
        onIssuesOpen={this.props.handleOpenIssues}
        onLinePopupToggle={this.props.handleLinePopupToggle}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={this.props.handleSymbolClick}
        openIssues={this.props.openIssuesByLine[line.line]}
        previousLine={index > 0 ? snippet[index - 1] : undefined}
        renderDuplicationPopup={this.props.renderDuplicationPopup}
        scroll={this.doScroll}
        secondaryIssueLocations={secondaryIssueLocations}
        selectedIssue={optimizeSelectedIssue(this.props.issue.key, issuesForLine)}
        verticalBuffer={verticalBuffer}
      />
    );
  }

  render() {
    const {
      component,
      issue,
      issuesByLine = {},
      last,
      locationsByLine,
      openIssuesByLine,
      snippet
    } = this.props;
    const lastLine =
      component.measures && component.measures.lines && parseInt(component.measures.lines, 10);

    const symbols = symbolsByLine(snippet);

    const bottomLine = snippet[snippet.length - 1].line;
    const issueLine = issue.textRange ? issue.textRange.endLine : issue.line;
    const lowestVisibleIssue = Math.max(
      ...Object.keys(issuesByLine)
        .map(k => parseInt(k, 10))
        .filter(l => inSnippet(l, snippet) && (l === issueLine || openIssuesByLine[l]))
    );
    const verticalBuffer = last
      ? Math.max(0, LINES_BELOW_LAST - (bottomLine - lowestVisibleIssue))
      : 0;

    const displayDuplications = snippet.some(s => !!s.duplicated);

    return (
      <div className="source-viewer-code snippet" ref={this.node}>
        <div>
          {snippet[0].line > 1 && (
            <div className="expand-block expand-block-above">
              <button
                aria-label={translate('source_viewer.expand_above')}
                onClick={this.expandBlock('up')}
                type="button">
                <ExpandSnippetIcon />
              </button>
            </div>
          )}
          <table
            className={classNames('source-table', {
              'expand-up': snippet[0].line > 1,
              'expand-down': !lastLine || snippet[snippet.length - 1].line < lastLine
            })}>
            <tbody>
              {snippet.map((line, index) =>
                this.renderLine({
                  displayDuplications,
                  index,
                  issuesForLine: issuesByLine[line.line] || [],
                  issueLocations: locationsByLine[line.line] || [],
                  line,
                  snippet,
                  symbols: symbols[line.line],
                  verticalBuffer: index === snippet.length - 1 ? verticalBuffer : 0
                })
              )}
            </tbody>
          </table>
          {(!lastLine || snippet[snippet.length - 1].line < lastLine) && (
            <div className="expand-block expand-block-below">
              <button
                aria-label={translate('source_viewer.expand_below')}
                onClick={this.expandBlock('down')}
                type="button">
                <ExpandSnippetIcon />
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }
}
