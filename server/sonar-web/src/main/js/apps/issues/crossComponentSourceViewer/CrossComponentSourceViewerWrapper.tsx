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
import { findLastIndex, keyBy } from 'lodash';
import * as React from 'react';
import { getComponentForSourceViewer, getDuplications, getSources } from '../../../api/components';
import { getIssueFlowSnippets } from '../../../api/issues';
import DuplicationPopup from '../../../components/SourceViewer/components/DuplicationPopup';
import {
  filterDuplicationBlocksByLine,
  getDuplicationBlocksForIndex,
  isDuplicationBlockInRemovedComponent
} from '../../../components/SourceViewer/helpers/duplications';
import {
  duplicationsByLine as getDuplicationsByLine,
  issuesByComponentAndLine
} from '../../../components/SourceViewer/helpers/indexing';
import { SourceViewerContext } from '../../../components/SourceViewer/SourceViewerContext';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { WorkspaceContext } from '../../../components/workspace/context';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { throwGlobalError } from '../../../helpers/error';
import { translate } from '../../../helpers/l10n';
import { HttpStatus } from '../../../helpers/request';
import { BranchLike } from '../../../types/branch-like';
import { isFile } from '../../../types/component';
import {
  Dict,
  DuplicatedFile,
  Duplication,
  FlowLocation,
  Issue,
  SnippetsByComponent,
  SourceViewerFile
} from '../../../types/types';
import ComponentSourceSnippetGroupViewer from './ComponentSourceSnippetGroupViewer';
import { getPrimaryLocation, groupLocationsByComponent } from './utils';

interface Props {
  branchLike: BranchLike | undefined;
  highlightedLocationMessage?: { index: number; text: string | undefined };
  issue: Issue;
  issues: Issue[];
  locations: FlowLocation[];
  onIssueChange: (issue: Issue) => void;
  onIssueSelect: (issueKey: string) => void;
  onLoaded?: () => void;
  onLocationSelect: (index: number) => void;
  scroll?: (element: HTMLElement) => void;
  selectedFlowIndex: number | undefined;
}

interface State {
  components: Dict<SnippetsByComponent>;
  duplicatedFiles?: Dict<DuplicatedFile>;
  duplications?: Duplication[];
  duplicationsByLine: { [line: number]: number[] };
  issuePopup?: { issue: string; name: string };
  loading: boolean;
  notAccessible: boolean;
}

export default class CrossComponentSourceViewerWrapper extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    components: {},
    duplicationsByLine: {},
    loading: true,
    notAccessible: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchIssueFlowSnippets();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.issue.key !== this.props.issue.key) {
      this.fetchIssueFlowSnippets();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDuplications = (component: string) => {
    getDuplications({
      key: component,
      ...getBranchLikeQuery(this.props.branchLike)
    }).then(
      r => {
        if (this.mounted) {
          this.setState({
            duplicatedFiles: r.files,
            duplications: r.duplications,
            duplicationsByLine: getDuplicationsByLine(r.duplications)
          });
        }
      },
      () => {}
    );
  };

  async fetchIssueFlowSnippets() {
    const { issue, branchLike } = this.props;
    this.setState({ loading: true });

    try {
      const components = await getIssueFlowSnippets(issue.key);
      if (components[issue.component] === undefined) {
        const issueComponent = await getComponentForSourceViewer({
          component: issue.component,
          ...getBranchLikeQuery(branchLike)
        });
        components[issue.component] = { component: issueComponent, sources: [] };
        if (isFile(issueComponent.q)) {
          const sources = await getSources({
            key: issueComponent.key,
            ...getBranchLikeQuery(branchLike),
            from: 1,
            to: 10
          }).then(lines => keyBy(lines, 'line'));
          components[issue.component].sources = sources;
        }
      }
      if (this.mounted) {
        this.setState({
          components,
          issuePopup: undefined,
          loading: false
        });
        if (this.props.onLoaded) {
          this.props.onLoaded();
        }
      }
    } catch (response) {
      const rsp = response as Response;
      if (rsp.status !== HttpStatus.Forbidden) {
        throwGlobalError(response);
      }
      if (this.mounted) {
        this.setState({ loading: false, notAccessible: rsp.status === HttpStatus.Forbidden });
      }
    }
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

  renderDuplicationPopup = (component: SourceViewerFile, index: number, line: number) => {
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
            openComponent={openComponent}
            sourceViewerFile={component}
          />
        )}
      </WorkspaceContext.Consumer>
    );
  };

  render() {
    const { loading, notAccessible } = this.state;

    if (loading) {
      return (
        <div>
          <DeferredSpinner />
        </div>
      );
    }

    if (notAccessible) {
      return (
        <Alert className="spacer-top" variant="warning">
          {translate('code_viewer.no_source_code_displayed_due_to_security')}
        </Alert>
      );
    }

    const { issue, locations } = this.props;
    const { components, duplications, duplicationsByLine } = this.state;
    const issuesByComponent = issuesByComponentAndLine(this.props.issues);
    const locationsByComponent = groupLocationsByComponent(issue, locations, components);

    const lastOccurenceOfPrimaryComponent = findLastIndex(
      locationsByComponent,
      ({ component }) => component.key === issue.component
    );

    if (components[issue.component] === undefined) {
      return null;
    }

    return (
      <div>
        {locationsByComponent.map((snippetGroup, i) => {
          return (
            <SourceViewerContext.Provider
              // eslint-disable-next-line react/no-array-index-key
              key={`${issue.key}-${this.props.selectedFlowIndex || 0}-${i}`}
              value={{ branchLike: this.props.branchLike, file: snippetGroup.component }}>
              <ComponentSourceSnippetGroupViewer
                branchLike={this.props.branchLike}
                duplications={duplications}
                duplicationsByLine={duplicationsByLine}
                highlightedLocationMessage={this.props.highlightedLocationMessage}
                issue={issue}
                issuePopup={this.state.issuePopup}
                issuesByLine={issuesByComponent[snippetGroup.component.key] || {}}
                isLastOccurenceOfPrimaryComponent={i === lastOccurenceOfPrimaryComponent}
                lastSnippetGroup={i === locationsByComponent.length - 1}
                loadDuplications={this.fetchDuplications}
                locations={snippetGroup.locations || []}
                onIssueChange={this.props.onIssueChange}
                onIssueSelect={this.props.onIssueSelect}
                onIssuePopupToggle={this.handleIssuePopupToggle}
                onLocationSelect={this.props.onLocationSelect}
                renderDuplicationPopup={this.renderDuplicationPopup}
                scroll={this.props.scroll}
                snippetGroup={snippetGroup}
              />
            </SourceViewerContext.Provider>
          );
        })}

        {locationsByComponent.length === 0 && (
          <ComponentSourceSnippetGroupViewer
            branchLike={this.props.branchLike}
            duplications={duplications}
            duplicationsByLine={duplicationsByLine}
            highlightedLocationMessage={this.props.highlightedLocationMessage}
            issue={issue}
            issuePopup={this.state.issuePopup}
            issuesByLine={issuesByComponent[issue.component] || {}}
            isLastOccurenceOfPrimaryComponent={true}
            lastSnippetGroup={true}
            loadDuplications={this.fetchDuplications}
            locations={[]}
            onIssueChange={this.props.onIssueChange}
            onIssueSelect={this.props.onIssueSelect}
            onIssuePopupToggle={this.handleIssuePopupToggle}
            onLocationSelect={this.props.onLocationSelect}
            renderDuplicationPopup={this.renderDuplicationPopup}
            scroll={this.props.scroll}
            snippetGroup={{
              locations: [getPrimaryLocation(issue)],
              ...components[issue.component]
            }}
          />
        )}
      </div>
    );
  }
}
