/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { noop } from 'lodash';
import * as React from 'react';
import { getSources } from '../../../api/components';
import Issue from '../../../components/issue/Issue';
import getCoverageStatus from '../../../components/SourceViewer/helpers/getCoverageStatus';
import { locationsByLine } from '../../../components/SourceViewer/helpers/indexing';
import SourceViewerHeaderSlim from '../../../components/SourceViewer/SourceViewerHeaderSlim';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import {
  Dict,
  Duplication,
  ExpandDirection,
  FlowLocation,
  Issue as TypeIssue,
  IssuesByLine,
  LinearIssueLocation,
  Snippet,
  SnippetGroup,
  SourceLine,
  SourceViewerFile
} from '../../../types/types';
import SnippetViewer from './SnippetViewer';
import {
  createSnippets,
  expandSnippet,
  EXPAND_BY_LINES,
  linesForSnippets,
  MERGE_DISTANCE
} from './utils';

interface Props {
  branchLike: BranchLike | undefined;
  duplications?: Duplication[];
  duplicationsByLine?: { [line: number]: number[] };
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  isLastOccurenceOfPrimaryComponent: boolean;
  issue: TypeIssue;
  issuePopup?: { issue: string; name: string };
  issuesByLine: IssuesByLine;
  lastSnippetGroup: boolean;
  loadDuplications: (component: string, line: SourceLine) => void;
  locations: FlowLocation[];
  onIssueChange: (issue: TypeIssue) => void;
  onIssuePopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onLocationSelect: (index: number) => void;
  renderDuplicationPopup: (
    component: SourceViewerFile,
    index: number,
    line: number
  ) => React.ReactNode;
  scroll?: (element: HTMLElement, offset: number) => void;
  snippetGroup: SnippetGroup;
}

interface State {
  additionalLines: { [line: number]: SourceLine };
  highlightedSymbols: string[];
  loading: boolean;
  openIssuesByLine: Dict<boolean>;
  snippets: Snippet[];
}

export default class ComponentSourceSnippetGroupViewer extends React.PureComponent<Props, State> {
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
    const { issue, snippetGroup } = this.props;

    const snippets = createSnippets({
      component: snippetGroup.component.key,
      issue,
      locations: snippetGroup.locations
    });

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

  /*
   * Clean after animation
   */
  cleanDom(index: number) {
    const nodes = this.getNodes(index);

    if (!nodes) {
      return;
    }

    const { wrapper, table } = nodes;

    table.style.marginTop = '';
    wrapper.style.maxHeight = '';
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

  expandBlock = (snippetIndex: number, direction: ExpandDirection): Promise<void> => {
    const { branchLike, snippetGroup } = this.props;
    const { key } = snippetGroup.component;
    const { snippets } = this.state;
    const snippet = snippets.find(s => s.index === snippetIndex);
    if (!snippet) {
      return Promise.reject();
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
    return getSources({
      key,
      ...range,
      ...getBranchLikeQuery(branchLike)
    })
      .then(lines =>
        lines.reduce((lineMap: Dict<SourceLine>, line) => {
          line.coverageStatus = getCoverageStatus(line);
          lineMap[line.line] = line;
          return lineMap;
        }, {})
      )
      .then(newLinesMapped => this.animateBlockExpansion(snippetIndex, direction, newLinesMapped));
  };

  animateBlockExpansion(
    snippetIndex: number,
    direction: ExpandDirection,
    newLinesMapped: Dict<SourceLine>
  ): Promise<void> {
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

      return new Promise(resolve => {
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
              this.setState({ snippets: newSnippets.filter(s => !s.toDelete) }, resolve);
              this.cleanDom(snippetIndex);
            }, 200);
          }
        );
      });
    }
    return Promise.resolve();
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

  handleOpenIssues = (line: SourceLine) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: true }
    }));
  };

  handleCloseIssues = (line: SourceLine) => {
    this.setState(state => ({
      openIssuesByLine: { ...state.openIssuesByLine, [line.line]: false }
    }));
  };

  handleSymbolClick = (clickedSymbols: string[]) => {
    this.setState(({ highlightedSymbols }) => {
      const newHighlightedSymbols = clickedSymbols.filter(
        symb => !highlightedSymbols.includes(symb)
      );
      return { highlightedSymbols: newHighlightedSymbols };
    });
  };

  loadDuplications = (line: SourceLine) => {
    this.props.loadDuplications(this.props.snippetGroup.component.key, line);
  };

  renderDuplicationPopup = (index: number, line: number) => {
    return this.props.renderDuplicationPopup(this.props.snippetGroup.component, index, line);
  };

  renderSnippet({
    index,
    issuesByLine,
    lastSnippetOfLastGroup,
    locationsByLine,
    snippet
  }: {
    index: number;
    issuesByLine: IssuesByLine;
    lastSnippetOfLastGroup: boolean;
    locationsByLine: { [line: number]: LinearIssueLocation[] };
    snippet: SourceLine[];
  }) {
    return (
      <SnippetViewer
        branchLike={this.props.branchLike}
        component={this.props.snippetGroup.component}
        duplications={this.props.duplications}
        duplicationsByLine={this.props.duplicationsByLine}
        expandBlock={this.expandBlock}
        handleCloseIssues={this.handleCloseIssues}
        handleOpenIssues={this.handleOpenIssues}
        handleSymbolClick={this.handleSymbolClick}
        highlightedLocationMessage={this.props.highlightedLocationMessage}
        highlightedSymbols={this.state.highlightedSymbols}
        index={index}
        issue={this.props.issue}
        issuePopup={this.props.issuePopup}
        issuesByLine={issuesByLine}
        lastSnippetOfLastGroup={lastSnippetOfLastGroup}
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
    const {
      branchLike,
      isLastOccurenceOfPrimaryComponent,
      issue,
      issuesByLine,
      issuePopup,
      lastSnippetGroup,
      snippetGroup
    } = this.props;
    const { additionalLines, loading, snippets } = this.state;
    const locations =
      issue.component === snippetGroup.component.key && issue.textRange !== undefined
        ? locationsByLine([issue])
        : {};

    const fullyShown =
      snippets.length === 1 &&
      snippetGroup.component.measures &&
      snippets[0].end - snippets[0].start ===
        parseInt(snippetGroup.component.measures.lines || '', 10);

    const snippetLines = linesForSnippets(snippets, {
      ...snippetGroup.sources,
      ...additionalLines
    });

    const isFlow = issue.secondaryLocations.length === 0;
    const includeIssueLocation = isFlow ? isLastOccurenceOfPrimaryComponent : true;

    return (
      <div className="component-source-container" ref={this.rootNodeRef}>
        <SourceViewerHeaderSlim
          branchLike={branchLike}
          expandable={!fullyShown}
          loading={loading}
          onExpand={this.expandComponent}
          sourceViewerFile={snippetGroup.component}
        />
        {issue.component === snippetGroup.component.key && issue.textRange === undefined && (
          <div className="padded-top padded-left padded-right">
            <Issue
              issue={issue}
              onChange={this.props.onIssueChange}
              onClick={noop}
              onPopupToggle={this.props.onIssuePopupToggle}
              openPopup={issuePopup && issuePopup.issue === issue.key ? issuePopup.name : undefined}
              selected={true}
            />
          </div>
        )}
        {snippetLines.map((snippet, index) => (
          <div id={`snippet-wrapper-${snippets[index].index}`} key={snippets[index].index}>
            {this.renderSnippet({
              snippet,
              index: snippets[index].index,
              issuesByLine,
              locationsByLine: includeIssueLocation ? locations : {},
              lastSnippetOfLastGroup: lastSnippetGroup && index === snippets.length - 1
            })}
          </div>
        ))}
      </div>
    );
  }
}
