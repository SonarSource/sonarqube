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
import * as React from 'react';
import { getSources } from '../../../api/components';
import IssueMessageBox from '../../../components/issue/IssueMessageBox';
import getCoverageStatus from '../../../components/SourceViewer/helpers/getCoverageStatus';
import { locationsByLine } from '../../../components/SourceViewer/helpers/indexing';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { isFile } from '../../../types/component';
import {
  Dict,
  Duplication,
  ExpandDirection,
  FlowLocation,
  Issue as TypeIssue,
  IssuesByLine,
  Snippet,
  SnippetGroup,
  SourceLine,
  SourceViewerFile,
} from '../../../types/types';
import { IssueSourceViewerScrollContext } from '../components/IssueSourceViewerScrollContext';
import IssueSourceViewerHeader from './IssueSourceViewerHeader';
import SnippetViewer from './SnippetViewer';
import {
  createSnippets,
  expandSnippet,
  EXPAND_BY_LINES,
  getPrimaryLocation,
  linesForSnippets,
  MERGE_DISTANCE,
} from './utils';

interface Props {
  branchLike: BranchLike | undefined;
  duplications?: Duplication[];
  duplicationsByLine?: { [line: number]: number[] };
  highlightedLocationMessage: { index: number; text: string | undefined } | undefined;
  isLastOccurenceOfPrimaryComponent: boolean;
  issue: TypeIssue;
  issuesByLine: IssuesByLine;
  lastSnippetGroup: boolean;
  loadDuplications: (component: string, line: SourceLine) => void;
  locations: FlowLocation[];
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: (index: number) => void;
  renderDuplicationPopup: (
    component: SourceViewerFile,
    index: number,
    line: number
  ) => React.ReactNode;
  snippetGroup: SnippetGroup;
}

interface State {
  additionalLines: { [line: number]: SourceLine };
  highlightedSymbols: string[];
  loading: boolean;
  snippets: Snippet[];
}

export default class ComponentSourceSnippetGroupViewer extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      additionalLines: {},
      highlightedSymbols: [],
      loading: false,
      snippets: [],
    };
  }

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
      locations:
        snippetGroup.locations.length === 0
          ? [getPrimaryLocation(issue)]
          : [getPrimaryLocation(issue), ...snippetGroup.locations],
    });

    this.setState({ snippets });
  }

  expandBlock = (snippetIndex: number, direction: ExpandDirection): Promise<void> => {
    const { branchLike, snippetGroup } = this.props;
    const { key } = snippetGroup.component;
    const { snippets } = this.state;
    const snippet = snippets.find((s) => s.index === snippetIndex);
    if (!snippet) {
      return Promise.reject();
    }
    // Extend by EXPAND_BY_LINES and add buffer for merging snippets
    const extension = EXPAND_BY_LINES + MERGE_DISTANCE - 1;
    const range =
      direction === 'up'
        ? {
            from: Math.max(1, snippet.start - extension),
            to: snippet.start - 1,
          }
        : {
            from: snippet.end + 1,
            to: snippet.end + extension,
          };
    return getSources({
      key,
      ...range,
      ...getBranchLikeQuery(branchLike),
    })
      .then((lines) =>
        lines.reduce((lineMap: Dict<SourceLine>, line) => {
          line.coverageStatus = getCoverageStatus(line);
          lineMap[line.line] = line;
          return lineMap;
        }, {})
      )
      .then((newLinesMapped) => {
        const newSnippets = expandSnippet({
          direction,
          snippetIndex,
          snippets,
        });

        this.setState(({ additionalLines }) => {
          const combinedLines = { ...additionalLines, ...newLinesMapped };
          return {
            additionalLines: combinedLines,
            snippets: newSnippets.filter((s) => !s.toDelete),
          };
        });
      });
  };

  expandComponent = () => {
    const { branchLike, snippetGroup } = this.props;
    const { key } = snippetGroup.component;

    this.setState({ loading: true });

    getSources({ key, ...getBranchLikeQuery(branchLike) }).then(
      (lines) => {
        if (this.mounted) {
          this.setState(({ additionalLines }) => {
            const combinedLines = { ...additionalLines, ...lines };
            return {
              additionalLines: combinedLines,
              loading: false,
              snippets: [{ start: 0, end: lines[lines.length - 1].line, index: -1 }],
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

  handleSymbolClick = (clickedSymbols: string[]) => {
    this.setState(({ highlightedSymbols }) => {
      const newHighlightedSymbols = clickedSymbols.filter(
        (symb) => !highlightedSymbols.includes(symb)
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

  renderIssuesList = (line: SourceLine) => {
    const { isLastOccurenceOfPrimaryComponent, issue, issuesByLine, snippetGroup } = this.props;
    const locations =
      issue.component === snippetGroup.component.key && issue.textRange !== undefined
        ? locationsByLine([issue])
        : {};

    const isFlow = issue.secondaryLocations.length === 0;
    const includeIssueLocation = isFlow ? isLastOccurenceOfPrimaryComponent : true;
    const issuesForLine = issuesByLine[line.line] || [];
    const issueLocations = includeIssueLocation ? locations[line.line] : [];

    return (
      issuesForLine.length > 0 && (
        <div>
          {issuesForLine.map((issueToDisplay) => {
            const isSelectedIssue = issueToDisplay.key === issue.key;
            return (
              <IssueSourceViewerScrollContext.Consumer key={issueToDisplay.key}>
                {(ctx) => (
                  <IssueMessageBox
                    selected={!!(isSelectedIssue && issueLocations.length > 0)}
                    issue={issueToDisplay}
                    onClick={this.props.onIssueSelect}
                    ref={isSelectedIssue ? ctx?.registerPrimaryLocationRef : undefined}
                  />
                )}
              </IssueSourceViewerScrollContext.Consumer>
            );
          })}
        </div>
      )
    );
  };

  render() {
    const { branchLike, isLastOccurenceOfPrimaryComponent, issue, lastSnippetGroup, snippetGroup } =
      this.props;
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
      ...additionalLines,
    });

    const isFlow = issue.secondaryLocations.length === 0;
    const includeIssueLocation = isFlow ? isLastOccurenceOfPrimaryComponent : true;

    return (
      <>
        <IssueSourceViewerHeader
          branchLike={branchLike}
          expandable={!fullyShown && isFile(snippetGroup.component.q)}
          loading={loading}
          onExpand={this.expandComponent}
          sourceViewerFile={snippetGroup.component}
        />

        {issue.component === snippetGroup.component.key && issue.textRange === undefined && (
          <IssueSourceViewerScrollContext.Consumer>
            {(ctx) => (
              <IssueMessageBox
                selected={true}
                issue={issue}
                onClick={this.props.onIssueSelect}
                ref={ctx?.registerPrimaryLocationRef}
              />
            )}
          </IssueSourceViewerScrollContext.Consumer>
        )}
        {snippetLines.map((snippet, index) => (
          <SnippetViewer
            key={snippets[index].index}
            renderAdditionalChildInLine={this.renderIssuesList}
            component={this.props.snippetGroup.component}
            duplications={this.props.duplications}
            duplicationsByLine={this.props.duplicationsByLine}
            expandBlock={this.expandBlock}
            handleSymbolClick={this.handleSymbolClick}
            highlightedLocationMessage={this.props.highlightedLocationMessage}
            highlightedSymbols={this.state.highlightedSymbols}
            index={snippets[index].index}
            issue={this.props.issue}
            lastSnippetOfLastGroup={lastSnippetGroup && index === snippets.length - 1}
            loadDuplications={this.loadDuplications}
            locations={this.props.locations}
            locationsByLine={includeIssueLocation ? locations : {}}
            onLocationSelect={this.props.onLocationSelect}
            renderDuplicationPopup={this.renderDuplicationPopup}
            snippet={snippet}
          />
        ))}
      </>
    );
  }
}
