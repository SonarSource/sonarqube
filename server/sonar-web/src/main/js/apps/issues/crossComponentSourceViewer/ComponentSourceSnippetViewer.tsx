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
import {
  createSnippets,
  expandSnippet,
  inSnippet,
  EXPAND_BY_LINES,
  LINES_BELOW_LAST,
  MERGE_DISTANCE
} from './utils';
import { getSources } from '../../../api/components';
import ExpandSnippetIcon from '../../../components/icons-components/ExpandSnippetIcon';
import Line from '../../../components/SourceViewer/components/Line';
import SourceViewerHeaderSlim from '../../../components/SourceViewer/SourceViewerHeaderSlim';
import getCoverageStatus from '../../../components/SourceViewer/helpers/getCoverageStatus';
import { symbolsByLine, locationsByLine } from '../../../components/SourceViewer/helpers/indexing';
import { getSecondaryIssueLocationsForLine } from '../../../components/SourceViewer/helpers/issueLocations';
import {
  optimizeLocationMessage,
  optimizeHighlightedSymbols,
  optimizeSelectedIssue
} from '../../../components/SourceViewer/helpers/lines';
import { translate } from '../../../helpers/l10n';

interface Props {
  branchLike: T.BranchLike | undefined;
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  issue: T.Issue;
  issuePopup?: { issue: string; name: string };
  issuesByLine: T.IssuesByLine;
  last: boolean;
  locations: T.FlowLocation[];
  onIssueChange: (issue: T.Issue) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onLocationSelect: (index: number) => void;
  renderDuplicationPopup: (index: number, line: number) => JSX.Element;
  scroll?: (element: HTMLElement) => void;
  snippetGroup: T.SnippetGroup;
}

interface State {
  additionalLines: { [line: number]: T.SourceLine };
  highlightedSymbols: string[];
  loading: boolean;
  openIssuesByLine: T.Dict<boolean>;
  snippets: T.SourceLine[][];
}

export default class ComponentSourceSnippetViewer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    additionalLines: {},
    highlightedSymbols: [],
    loading: false,
    openIssuesByLine: {},
    snippets: []
  };

  componentDidMount() {
    this.mounted = true;
    this.createSnippetsFromProps();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  createSnippetsFromProps() {
    const mainLocation: T.FlowLocation = {
      component: this.props.issue.component,
      textRange: this.props.issue.textRange || {
        endLine: 0,
        endOffset: 0,
        startLine: 0,
        startOffset: 0
      }
    };
    const snippets = createSnippets(
      this.props.snippetGroup.locations.concat(mainLocation),
      this.props.snippetGroup.sources,
      this.props.last
    );
    this.setState({ snippets });
  }

  expandBlock = (snippetIndex: number, direction: T.ExpandDirection) => {
    const { snippets } = this.state;

    const snippet = snippets[snippetIndex];

    // Extend by EXPAND_BY_LINES and add buffer for merging snippets
    const extension = EXPAND_BY_LINES + MERGE_DISTANCE - 1;

    const range =
      direction === 'up'
        ? {
            from: Math.max(1, snippet[0].line - extension),
            to: snippet[0].line - 1
          }
        : {
            from: snippet[snippet.length - 1].line + 1,
            to: snippet[snippet.length - 1].line + extension
          };

    getSources({
      key: this.props.snippetGroup.component.key,
      ...range
    })
      .then(lines =>
        lines.reduce((lineMap: T.Dict<T.SourceLine>, line) => {
          line.coverageStatus = getCoverageStatus(line);
          lineMap[line.line] = line;
          return lineMap;
        }, {})
      )
      .then(
        newLinesMapped => {
          if (this.mounted) {
            this.setState(({ additionalLines, snippets }) => {
              const combinedLines = { ...additionalLines, ...newLinesMapped };

              return {
                additionalLines: combinedLines,
                snippets: expandSnippet({
                  direction,
                  lines: { ...combinedLines, ...this.props.snippetGroup.sources },
                  snippetIndex,
                  snippets
                })
              };
            });
          }
        },
        () => null
      );
  };

  expandComponent = () => {
    const { key } = this.props.snippetGroup.component;

    this.setState({ loading: true });

    getSources({ key }).then(
      lines => {
        if (this.mounted) {
          this.setState({ loading: false, snippets: [lines] });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
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

  renderLine({
    index,
    issuesForLine,
    issueLocations,
    line,
    snippet,
    symbols,
    verticalBuffer
  }: {
    index: number;
    issuesForLine: T.Issue[];
    issueLocations: T.LinearIssueLocation[];
    line: T.SourceLine;
    snippet: T.SourceLine[];
    symbols: string[];
    verticalBuffer: number;
  }) {
    const { openIssuesByLine } = this.state;

    const secondaryIssueLocations = getSecondaryIssueLocationsForLine(line, this.props.locations);

    const noop = () => {};

    const isSinkLine = issuesForLine.some(i => i.key === this.props.issue.key);

    return (
      <Line
        branchLike={undefined}
        displayAllIssues={false}
        displayCoverage={true}
        displayDuplications={false}
        displayIssues={!isSinkLine || issuesForLine.length > 1}
        displayLocationMarkers={true}
        duplications={[]}
        duplicationsCount={0}
        highlighted={false}
        highlightedLocationMessage={optimizeLocationMessage(
          this.props.highlightedLocationMessage,
          secondaryIssueLocations
        )}
        highlightedSymbols={optimizeHighlightedSymbols(symbols, this.state.highlightedSymbols)}
        issueLocations={issueLocations}
        issuePopup={this.props.issuePopup}
        issues={issuesForLine}
        key={line.line}
        last={false}
        line={line}
        linePopup={undefined}
        loadDuplications={noop}
        onIssueChange={this.props.onIssueChange}
        onIssuePopupToggle={this.props.onIssuePopupToggle}
        onIssueSelect={noop}
        onIssueUnselect={noop}
        onIssuesClose={this.handleCloseIssues}
        onIssuesOpen={this.handleOpenIssues}
        onLinePopupToggle={noop}
        onLocationSelect={this.props.onLocationSelect}
        onSymbolClick={highlightedSymbols => this.setState({ highlightedSymbols })}
        openIssues={openIssuesByLine[line.line]}
        previousLine={index > 0 ? snippet[index - 1] : undefined}
        renderDuplicationPopup={this.props.renderDuplicationPopup}
        scroll={this.props.scroll}
        secondaryIssueLocations={secondaryIssueLocations}
        selectedIssue={optimizeSelectedIssue(this.props.issue.key, issuesForLine)}
        verticalBuffer={verticalBuffer}
      />
    );
  }

  renderSnippet({
    snippet,
    index,
    issue,
    issuesByLine = {},
    locationsByLine,
    last
  }: {
    snippet: T.SourceLine[];
    index: number;
    issue: T.Issue;
    issuesByLine: T.IssuesByLine;
    locationsByLine: { [line: number]: T.LinearIssueLocation[] };
    last: boolean;
  }) {
    const { component } = this.props.snippetGroup;
    const lastLine =
      component.measures && component.measures.lines && parseInt(component.measures.lines, 10);

    const symbols = symbolsByLine(snippet);

    const expandBlock = (direction: T.ExpandDirection) => () => this.expandBlock(index, direction);

    const bottomLine = snippet[snippet.length - 1].line;
    const issueLine = issue.textRange ? issue.textRange.endLine : issue.line;
    const lowestVisibleIssue = Math.max(
      ...Object.keys(issuesByLine)
        .map(k => parseInt(k, 10))
        .filter(l => inSnippet(l, snippet) && (l === issueLine || this.state.openIssuesByLine[l]))
    );
    const verticalBuffer = last
      ? Math.max(0, LINES_BELOW_LAST - (bottomLine - lowestVisibleIssue))
      : 0;

    return (
      <div className="source-viewer-code snippet" key={index}>
        {snippet[0].line > 1 && (
          <button
            aria-label={translate('source_viewer.expand_above')}
            className="expand-block expand-block-above"
            onClick={expandBlock('up')}
            type="button">
            <ExpandSnippetIcon />
          </button>
        )}
        <table className="source-table">
          <tbody>
            {snippet.map((line, index) =>
              this.renderLine({
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
          <button
            aria-label={translate('source_viewer.expand_below')}
            className="expand-block expand-block-below"
            onClick={expandBlock('down')}
            type="button">
            <ExpandSnippetIcon />
          </button>
        )}
      </div>
    );
  }

  render() {
    const { branchLike, issue, issuesByLine, last, snippetGroup } = this.props;
    const { loading, snippets } = this.state;
    const locations = locationsByLine([issue]);

    const fullyShown =
      snippets.length === 1 &&
      snippetGroup.component.measures &&
      snippets[0].length === parseInt(snippetGroup.component.measures.lines || '', 10);

    return (
      <div className="component-source-container">
        <SourceViewerHeaderSlim
          branchLike={branchLike}
          expandable={!fullyShown}
          loading={loading}
          onExpand={this.expandComponent}
          sourceViewerFile={snippetGroup.component}
        />
        {snippets.map((snippet, index) =>
          this.renderSnippet({
            snippet,
            index,
            issue,
            issuesByLine: last ? issuesByLine : {},
            locationsByLine: last && index === snippets.length - 1 ? locations : {},
            last: last && index === snippets.length - 1
          })
        )}
      </div>
    );
  }
}
