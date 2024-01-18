/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Spinner } from 'design-system';
import { findLastIndex, keyBy } from 'lodash';
import * as React from 'react';
import { getComponentForSourceViewer, getDuplications, getSources } from '../../../api/components';
import { getIssueFlowSnippets } from '../../../api/issues';
import { SourceViewerContext } from '../../../components/SourceViewer/SourceViewerContext';
import DuplicationPopup from '../../../components/SourceViewer/components/DuplicationPopup';
import {
  filterDuplicationBlocksByLine,
  getDuplicationBlocksForIndex,
  isDuplicationBlockInRemovedComponent,
} from '../../../components/SourceViewer/helpers/duplications';
import {
  duplicationsByLine as getDuplicationsByLine,
  issuesByComponentAndLine,
} from '../../../components/SourceViewer/helpers/indexing';
import { Alert } from '../../../components/ui/Alert';
import { WorkspaceContext } from '../../../components/workspace/context';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { throwGlobalError } from '../../../helpers/error';
import { translate } from '../../../helpers/l10n';
import { HttpStatus } from '../../../helpers/request';
import { BranchLike } from '../../../types/branch-like';
import { isFile } from '../../../types/component';
import { IssueDeprecatedStatus } from '../../../types/issues';
import {
  Dict,
  DuplicatedFile,
  Duplication,
  FlowLocation,
  Issue,
  SnippetsByComponent,
  SourceViewerFile,
} from '../../../types/types';
import ComponentSourceSnippetGroupViewer from './ComponentSourceSnippetGroupViewer';
import { groupLocationsByComponent } from './utils';

interface Props {
  branchLike: BranchLike | undefined;
  highlightedLocationMessage?: { index: number; text: string | undefined };
  issue: Issue;
  issues: Issue[];
  locations: FlowLocation[];
  onIssueSelect: (issueKey: string) => void;
  onLocationSelect: (index: number) => void;
  selectedFlowIndex: number | undefined;
}

interface State {
  components: Dict<SnippetsByComponent>;
  duplicatedFiles?: Dict<DuplicatedFile>;
  duplications?: Duplication[];
  duplicationsByLine: { [line: number]: number[] };
  loading: boolean;
  notAccessible: boolean;
}

export default class CrossComponentSourceViewer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    components: {},
    duplicationsByLine: {},
    loading: true,
    notAccessible: false,
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
      ...getBranchLikeQuery(this.props.branchLike),
    }).then(
      (r) => {
        if (this.mounted) {
          this.setState({
            duplicatedFiles: r.files,
            duplications: r.duplications,
            duplicationsByLine: getDuplicationsByLine(r.duplications),
          });
        }
      },
      () => {
        /* No error hanlding here  */
      },
    );
  };

  async fetchIssueFlowSnippets() {
    const { issue, branchLike } = this.props;
    this.setState({ loading: true });

    try {
      const components =
        issue.status === IssueDeprecatedStatus.Closed ? {} : await getIssueFlowSnippets(issue.key);
      if (components[issue.component] === undefined) {
        const issueComponent = await getComponentForSourceViewer({
          // If the issue's component doesn't exist anymore (typically a deleted file), use the project
          component: issue.componentEnabled ? issue.component : issue.project,
          ...getBranchLikeQuery(branchLike),
        });
        components[issue.component] = { component: issueComponent, sources: [] };
        if (isFile(issueComponent.q)) {
          const sources = await getSources({
            key: issueComponent.key,
            ...getBranchLikeQuery(branchLike),
            from: 1,
            to: 10,
          }).then((lines) => keyBy(lines, 'line'));
          components[issue.component].sources = sources;
        }
      }

      if (this.mounted) {
        this.setState({
          components,
          loading: false,
        });
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
            inRemovedComponent={isDuplicationBlockInRemovedComponent(blocks)}
            duplicatedFiles={duplicatedFiles}
            openComponent={openComponent}
            sourceViewerFile={component}
            duplicationHeader={translate('component_viewer.transition.duplication')}
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
          <Spinner ariaLabel={translate('code_viewer.loading')} />
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

    const lastOccurenceOfPrimaryComponent = findLastIndex(locationsByComponent, ({ component }) =>
      component ? component.key === issue.component : true,
    );

    if (components[issue.component] === undefined) {
      return null;
    }

    return (
      <>
        {locationsByComponent.map((snippetGroup, i) => {
          return (
            <SourceViewerContext.Provider
              key={`${issue.key}-${this.props.selectedFlowIndex}-${snippetGroup.component.key}`}
              value={{ branchLike: this.props.branchLike, file: snippetGroup.component }}
            >
              <ComponentSourceSnippetGroupViewer
                branchLike={this.props.branchLike}
                duplications={duplications}
                duplicationsByLine={duplicationsByLine}
                highlightedLocationMessage={this.props.highlightedLocationMessage}
                issue={issue}
                issuesByLine={issuesByComponent[snippetGroup.component.key] || {}}
                isLastOccurenceOfPrimaryComponent={i === lastOccurenceOfPrimaryComponent}
                loadDuplications={this.fetchDuplications}
                locations={snippetGroup.locations || []}
                onIssueSelect={this.props.onIssueSelect}
                onLocationSelect={this.props.onLocationSelect}
                renderDuplicationPopup={this.renderDuplicationPopup}
                snippetGroup={snippetGroup}
              />
            </SourceViewerContext.Provider>
          );
        })}
      </>
    );
  }
}
