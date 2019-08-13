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
import * as classNames from 'classnames';
import * as React from 'react';
import { getSources } from '../../../api/components';
import getCoverageStatus from '../../../components/SourceViewer/helpers/getCoverageStatus';
import { locationsByLine } from '../../../components/SourceViewer/helpers/indexing';
import SourceViewerHeaderSlim from '../../../components/SourceViewer/SourceViewerHeaderSlim';
import { getBranchLikeQuery } from '../../../helpers/branches';
import SnippetViewer from './SnippetViewer';
import {
  createSnippets,
  expandSnippet,
  EXPAND_BY_LINES,
  linesForSnippets,
  MERGE_DISTANCE
} from './utils';

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
  snippets: T.Snippet[];
}

export default class ComponentSourceSnippetViewer extends React.PureComponent<Props, State> {
  mounted = false;
  rootNodeRef = React.createRef<HTMLDivElement>();
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
      this.props.last,
      this.props.issue.secondaryLocations.length > 0 ? this.props.issue : undefined
    );

    this.setState({ snippets });
  }

  getNodes(index: number): { wrapper: HTMLElement; table: HTMLElement } | undefined {
    const root = this.rootNodeRef.current;
    if (!root) {
      return undefined;
    }
    const element = root.querySelector(`#snippet-wrapper-${index}`);
    if (!element) {
      return undefined;
    }
    const wrapper = element.querySelector<HTMLElement>('.snippet');
    if (!wrapper) {
      return undefined;
    }
    const table = wrapper.firstChild as HTMLElement;
    if (!table) {
      return undefined;
    }

    return { wrapper, table };
  }

  setMaxHeight(index: number, value?: number, up = false) {
    const nodes = this.getNodes(index);

    if (!nodes) {
      return;
    }

    const { wrapper, table } = nodes;

    const maxHeight = value !== undefined ? value : table.getBoundingClientRect().height;

    if (up) {
      const startHeight = wrapper.getBoundingClientRect().height;
      table.style.transition = 'none';
      table.style.marginTop = `${startHeight - maxHeight}px`;

      // animate!
      setTimeout(() => {
        table.style.transition = '';
        table.style.marginTop = '0px';
        wrapper.style.maxHeight = `${maxHeight + 20}px`;
      }, 0);
    } else {
      wrapper.style.maxHeight = `${maxHeight + 20}px`;
    }
  }

  expandBlock = (snippetIndex: number, direction: T.ExpandDirection) => {
    const { branchLike, snippetGroup } = this.props;
    const { key } = snippetGroup.component;
    const { snippets } = this.state;
    const snippet = snippets.find(s => s.index === snippetIndex);
    if (!snippet) {
      return;
    }
    // Extend by EXPAND_BY_LINES and add buffer for merging snippets
    const extension = EXPAND_BY_LINES + MERGE_DISTANCE - 1;
    const range =
      direction === 'up'
        ? {
            from: Math.max(1, snippet.start - extension),
            to: snippet.start - 1
          }
        : {
            from: snippet.end + 1,
            to: snippet.end + extension
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
        newLinesMapped => this.animateBlockExpansion(snippetIndex, direction, newLinesMapped),
        () => {}
      );
  };

  animateBlockExpansion(
    snippetIndex: number,
    direction: T.ExpandDirection,
    newLinesMapped: T.Dict<T.SourceLine>
  ) {
    if (this.mounted) {
      const { snippets } = this.state;

      const newSnippets = expandSnippet({
        direction,
        snippetIndex,
        snippets
      });

      const deletedSnippets = newSnippets.filter(s => s.toDelete);

      // set max-height to current height for CSS transitions
      deletedSnippets.forEach(s => this.setMaxHeight(s.index));
      this.setMaxHeight(snippetIndex);

      this.setState(
        ({ additionalLines, snippets }) => {
          const combinedLines = { ...additionalLines, ...newLinesMapped };
          return {
            additionalLines: combinedLines,
            snippets
          };
        },
        () => {
          // Set max-height 0 to trigger CSS transitions
          deletedSnippets.forEach(s => {
            this.setMaxHeight(s.index, 0);
          });
          this.setMaxHeight(snippetIndex, undefined, direction === 'up');

          // Wait for transition to finish before updating dom
          setTimeout(() => {
            this.setState({ snippets: newSnippets.filter(s => !s.toDelete) });
          }, 200);
        }
      );
    }
  }

  expandComponent = () => {
    const { branchLike, snippetGroup } = this.props;
    const { key } = snippetGroup.component;

    this.setState({ loading: true });

    getSources({ key, ...getBranchLikeQuery(branchLike) }).then(
      lines => {
        if (this.mounted) {
          this.setState(({ additionalLines }) => {
            const combinedLines = { ...additionalLines, ...lines };
            return {
              additionalLines: combinedLines,
              loading: false,
              snippets: [{ start: 0, end: lines[lines.length - 1].line, index: -1 }]
            };
          });
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
    const { additionalLines, loading, snippets } = this.state;
    const locations = locationsByLine([issue]);

    const fullyShown =
      snippets.length === 1 &&
      snippetGroup.component.measures &&
      snippets[0].end - snippets[0].start ===
        parseInt(snippetGroup.component.measures.lines || '', 10);

    const snippetLines = linesForSnippets(snippets, {
      ...snippetGroup.sources,
      ...additionalLines
    });

    return (
      <div
        className={classNames('component-source-container', {
          'source-duplications-expanded': duplications && duplications.length > 0
        })}
        ref={this.rootNodeRef}>
        <SourceViewerHeaderSlim
          branchLike={branchLike}
          expandable={!fullyShown}
          loading={loading}
          onExpand={this.expandComponent}
          sourceViewerFile={snippetGroup.component}
        />
        {snippetLines.map((snippet, index) => (
          <div id={`snippet-wrapper-${snippets[index].index}`} key={snippets[index].index}>
            {this.renderSnippet({
              snippet,
              index: snippets[index].index,
              issuesByLine: last ? issuesByLine : {},
              locationsByLine: last && index === snippets.length - 1 ? locations : {},
              last: last && index === snippets.length - 1
            })}
          </div>
        ))}
      </div>
    );
  }
}
