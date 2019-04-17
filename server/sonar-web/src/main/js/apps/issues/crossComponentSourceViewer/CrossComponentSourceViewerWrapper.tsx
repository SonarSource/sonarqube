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
import ComponentSourceSnippetViewer from './ComponentSourceSnippetViewer';
import { groupLocationsByComponent } from './utils';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { getIssueFlowSnippets } from '../../../api/issues';
import { issuesByComponentAndLine } from '../../../components/SourceViewer/helpers/indexing';

interface State {
  components: T.Dict<T.SnippetsByComponent>;
  issuePopup?: { issue: string; name: string };
  loading: boolean;
}

interface Props {
  branchLike: T.Branch | T.PullRequest | undefined;
  highlightedLocationMessage?: { index: number; text: string | undefined };
  issue: T.Issue;
  issues: T.Issue[];
  locations: T.FlowLocation[];
  onIssueChange: (issue: T.Issue) => void;
  onLoaded?: () => void;
  onLocationSelect: (index: number) => void;
  renderDuplicationPopup: (index: number, line: number) => JSX.Element;
  scroll?: (element: HTMLElement) => void;
  selectedFlowIndex: number | undefined;
}

export default class CrossComponentSourceViewerWrapper extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    components: {},
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

  fetchIssueFlowSnippets(issueKey: string) {
    this.setState({ loading: true });
    getIssueFlowSnippets(issueKey).then(
      components => {
        if (this.mounted) {
          this.setState({ components, issuePopup: undefined, loading: false });
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

  render() {
    const { components, loading } = this.state;

    if (loading) {
      return (
        <div>
          <DeferredSpinner />
        </div>
      );
    }

    const issuesByComponent = issuesByComponentAndLine(this.props.issues);
    const locationsByComponent = groupLocationsByComponent(this.props.locations, components);

    return (
      <div>
        {locationsByComponent.map((g, i) => (
          <ComponentSourceSnippetViewer
            branchLike={this.props.branchLike}
            highlightedLocationMessage={this.props.highlightedLocationMessage}
            issue={this.props.issue}
            issuePopup={this.state.issuePopup}
            issuesByLine={issuesByComponent[g.component.key] || {}}
            key={this.props.issue.key + '-' + this.props.selectedFlowIndex + '-' + i}
            last={i === locationsByComponent.length - 1}
            locations={g.locations || []}
            onIssueChange={this.props.onIssueChange}
            onIssuePopupToggle={this.handleIssuePopupToggle}
            onLocationSelect={this.props.onLocationSelect}
            renderDuplicationPopup={this.props.renderDuplicationPopup}
            scroll={this.props.scroll}
            snippetGroup={g}
          />
        ))}
      </div>
    );
  }
}
