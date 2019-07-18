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
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { getDuplications } from '../../../api/components';
import { getIssueFlowSnippets } from '../../../api/issues';
import DuplicationPopup from '../../../components/SourceViewer/components/DuplicationPopup';
import {
  filterDuplicationBlocksByLine,
  getDuplicationBlocksForIndex,
  isDuplicationBlockInRemovedComponent
} from '../../../components/SourceViewer/helpers/duplications';
import {
  duplicationsByLine,
  issuesByComponentAndLine
} from '../../../components/SourceViewer/helpers/indexing';
import { SourceViewerContext } from '../../../components/SourceViewer/SourceViewerContext';
import { WorkspaceContext } from '../../../components/workspace/context';
import { getBranchLikeQuery } from '../../../helpers/branches';
import ComponentSourceSnippetViewer from './ComponentSourceSnippetViewer';
import { groupLocationsByComponent } from './utils';

interface Props {
  branchLike: T.Branch | T.PullRequest | undefined;
  highlightedLocationMessage?: { index: number; text: string | undefined };
  issue: T.Issue;
  issues: T.Issue[];
  locations: T.FlowLocation[];
  onIssueChange: (issue: T.Issue) => void;
  onLoaded?: () => void;
  onLocationSelect: (index: number) => void;
  scroll?: (element: HTMLElement) => void;
  selectedFlowIndex: number | undefined;
}

interface State {
  components: T.Dict<T.SnippetsByComponent>;
  duplicatedFiles?: T.Dict<T.DuplicatedFile>;
  duplications?: T.Duplication[];
  duplicationsByLine: { [line: number]: number[] };
  issuePopup?: { issue: string; name: string };
  linePopup?: T.LinePopup & { component: string };
  loading: boolean;
}

export default class CrossComponentSourceViewerWrapper extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    components: {},
    duplicationsByLine: {},
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchIssueFlowSnippets(this.props.issue.key);
  }

  componentWillReceiveProps(newProps: Props) {
    if (newProps.issue.key !== this.props.issue.key) {
      this.fetchIssueFlowSnippets(newProps.issue.key);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDuplications = (component: string, line: T.SourceLine) => {
    getDuplications({
      key: component,
      ...getBranchLikeQuery(this.props.branchLike)
    }).then(
      r => {
        if (this.mounted) {
          this.setState(state => ({
            duplicatedFiles: r.files,
            duplications: r.duplications,
            duplicationsByLine: duplicationsByLine(r.duplications),
            linePopup:
              r.duplications.length === 1
                ? { component, index: 0, line: line.line, name: 'duplications' }
                : state.linePopup
          }));
        }
      },
      () => {}
    );
  };

  fetchIssueFlowSnippets(issueKey: string) {
    this.setState({ loading: true });
    getIssueFlowSnippets(issueKey).then(
      components => {
        if (this.mounted) {
          this.setState({
            components,
            issuePopup: undefined,
            linePopup: undefined,
            loading: false
          });
          if (this.props.onLoaded) {
            this.props.onLoaded();
          }
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  handleIssuePopupToggle = (issue: string, popupName: string, open?: boolean) => {
    this.setState((state: State) => {
      const samePopup =
        state.issuePopup && state.issuePopup.name === popupName && state.issuePopup.issue === issue;
      if (open !== false && !samePopup) {
        return { issuePopup: { issue, name: popupName } };
      } else if (open !== true && samePopup) {
        return { issuePopup: undefined };
      }
      return null;
    });
  };

  handleLinePopupToggle = ({
    component,
    index,
    line,
    name,
    open
  }: T.LinePopup & { component: string }) => {
    this.setState((state: State) => {
      const samePopup =
        state.linePopup !== undefined &&
        state.linePopup.line === line &&
        state.linePopup.name === name &&
        state.linePopup.component === component &&
        state.linePopup.index === index;
      if (open !== false && !samePopup) {
        return { linePopup: { component, index, line, name } };
      } else if (open !== true && samePopup) {
        return { linePopup: undefined };
      }
      return null;
    });
  };

  handleCloseLinePopup = () => {
    this.setState({ linePopup: undefined });
  };

  renderDuplicationPopup = (component: T.SourceViewerFile, index: number, line: number) => {
    const { duplicatedFiles, duplications } = this.state;

    if (!component || !duplicatedFiles) {
      return null;
    }

    const blocks = getDuplicationBlocksForIndex(duplications, index);

    return (
      <WorkspaceContext.Consumer>
        {({ openComponent }) => (
          <DuplicationPopup
            blocks={filterDuplicationBlocksByLine(blocks, line)}
            branchLike={this.props.branchLike}
            duplicatedFiles={duplicatedFiles}
            inRemovedComponent={isDuplicationBlockInRemovedComponent(blocks)}
            onClose={this.handleCloseLinePopup}
            openComponent={openComponent}
            sourceViewerFile={component}
          />
        )}
      </WorkspaceContext.Consumer>
    );
  };

  render() {
    const { loading } = this.state;

    if (loading) {
      return (
        <div>
          <DeferredSpinner />
        </div>
      );
    }

    const { components, duplications, duplicationsByLine, linePopup } = this.state;
    const issuesByComponent = issuesByComponentAndLine(this.props.issues);
    const locationsByComponent = groupLocationsByComponent(this.props.locations, components);

    return (
      <div>
        {locationsByComponent.map((snippetGroup, i) => {
          let componentProps = {};
          if (linePopup && snippetGroup.component.key === linePopup.component) {
            componentProps = {
              duplications,
              duplicationsByLine,
              linePopup: { index: linePopup.index, line: linePopup.line, name: linePopup.name }
            };
          }
          return (
            <SourceViewerContext.Provider
              key={`${this.props.issue.key}-${this.props.selectedFlowIndex}-${i}`}
              value={{ branchLike: this.props.branchLike, file: snippetGroup.component }}>
              <ComponentSourceSnippetViewer
                branchLike={this.props.branchLike}
                highlightedLocationMessage={this.props.highlightedLocationMessage}
                issue={this.props.issue}
                issuePopup={this.state.issuePopup}
                issuesByLine={issuesByComponent[snippetGroup.component.key] || {}}
                last={i === locationsByComponent.length - 1}
                loadDuplications={this.fetchDuplications}
                locations={snippetGroup.locations || []}
                onIssueChange={this.props.onIssueChange}
                onIssuePopupToggle={this.handleIssuePopupToggle}
                onLinePopupToggle={this.handleLinePopupToggle}
                onLocationSelect={this.props.onLocationSelect}
                renderDuplicationPopup={this.renderDuplicationPopup}
                scroll={this.props.scroll}
                snippetGroup={snippetGroup}
                {...componentProps}
              />
            </SourceViewerContext.Provider>
          );
        })}
      </div>
    );
  }
}
