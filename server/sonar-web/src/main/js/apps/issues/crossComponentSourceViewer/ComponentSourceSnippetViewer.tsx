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
import { createSnippets, expandSnippet, EXPAND_BY_LINES, MERGE_DISTANCE } from './utils';
import SnippetViewer from './SnippetViewer';
import SourceViewerHeaderSlim from '../../../components/SourceViewer/SourceViewerHeaderSlim';
import getCoverageStatus from '../../../components/SourceViewer/helpers/getCoverageStatus';
import { getSources } from '../../../api/components';
import { locationsByLine } from '../../../components/SourceViewer/helpers/indexing';
import { getBranchLikeQuery } from '../../../helpers/branches';

interface Props {
  branchLike: T.BranchLike | undefined;
  duplications?: T.Duplication[];
  duplicationsByLine?: { [line: number]: number[] };
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  issue: T.Issue;
  issuePopup?: { issue: string; name: string };
  issuesByLine: T.IssuesByLine;
  last: boolean;
  linePopup?: T.LinePopup;
  loadDuplications: (component: string, line: T.SourceLine) => void;
  locations: T.FlowLocation[];
  onIssueChange: (issue: T.Issue) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onLinePopupToggle: (linePopup: T.LinePopup & { component: string }) => void;
  onLocationSelect: (index: number) => void;
  renderDuplicationPopup: (
    component: T.SourceViewerFile,
    index: number,
    line: number
  ) => React.ReactNode;
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
    const snippets = createSnippets(
      this.props.snippetGroup.locations,
      this.props.snippetGroup.sources,
      this.props.last
    );
    this.setState({ snippets });
  }

  expandBlock = (snippetIndex: number, direction: T.ExpandDirection) => {
    const { branchLike, snippetGroup } = this.props;
    const { key } = snippetGroup.component;
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
      key,
      ...range,
      ...getBranchLikeQuery(branchLike)
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
        () => {}
      );
  };

  expandComponent = () => {
    const { branchLike, snippetGroup } = this.props;
    const { key } = snippetGroup.component;

    this.setState({ loading: true });

    getSources({ key, ...getBranchLikeQuery(branchLike) }).then(
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

  handleLinePopupToggle = (linePopup: T.LinePopup) => {
    this.props.onLinePopupToggle({
      ...linePopup,
      component: this.props.snippetGroup.component.key
    });
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

  handleSymbolClick = (highlightedSymbols: string[]) => {
    this.setState({ highlightedSymbols });
  };

  loadDuplications = (line: T.SourceLine) => {
    this.props.loadDuplications(this.props.snippetGroup.component.key, line);
  };

  renderDuplicationPopup = (index: number, line: number) => {
    return this.props.renderDuplicationPopup(this.props.snippetGroup.component, index, line);
  };

  renderSnippet({
    index,
    issuesByLine,
    last,
    locationsByLine,
    snippet
  }: {
    index: number;
    issuesByLine: T.IssuesByLine;
    last: boolean;
    locationsByLine: { [line: number]: T.LinearIssueLocation[] };
    snippet: T.SourceLine[];
  }) {
    return (
      <SnippetViewer
        branchLike={this.props.branchLike}
        component={this.props.snippetGroup.component}
        expandBlock={this.expandBlock}
        handleCloseIssues={this.handleCloseIssues}
        handleLinePopupToggle={this.handleLinePopupToggle}
        handleOpenIssues={this.handleOpenIssues}
        handleSymbolClick={this.handleSymbolClick}
        highlightedLocationMessage={this.props.highlightedLocationMessage}
        highlightedSymbols={this.state.highlightedSymbols}
        index={index}
        issue={this.props.issue}
        issuePopup={this.props.issuePopup}
        issuesByLine={issuesByLine}
        key={index}
        last={last}
        loadDuplications={this.loadDuplications}
        locations={this.props.locations}
        locationsByLine={locationsByLine}
        onIssueChange={this.props.onIssueChange}
        onIssuePopupToggle={this.props.onIssuePopupToggle}
        onLocationSelect={this.props.onLocationSelect}
        openIssuesByLine={this.state.openIssuesByLine}
        renderDuplicationPopup={this.renderDuplicationPopup}
        scroll={this.props.scroll}
        snippet={snippet}
      />
    );
  }

  render() {
    const { branchLike, duplications, issue, issuesByLine, last, snippetGroup } = this.props;
    const { loading, snippets } = this.state;
    const locations = locationsByLine([issue]);

    const fullyShown =
      snippets.length === 1 &&
      snippetGroup.component.measures &&
      snippets[0].length === parseInt(snippetGroup.component.measures.lines || '', 10);

    return (
      <div
        className={classNames('component-source-container', {
          'source-duplications-expanded': duplications && duplications.length > 0
        })}>
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
            issuesByLine: last ? issuesByLine : {},
            locationsByLine: last && index === snippets.length - 1 ? locations : {},
            last: last && index === snippets.length - 1
          })
        )}
      </div>
    );
  }
}
