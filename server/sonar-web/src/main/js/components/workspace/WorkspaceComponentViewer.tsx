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
import { connect } from 'react-redux';
import { debounce } from 'lodash';
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import { getParents } from '../../api/components';
import { isPullRequest, isShortLivingBranch } from '../../helpers/branches';
import { fetchBranchStatus } from '../../store/rootActions';
import SourceViewer from '../SourceViewer/SourceViewer';
import { ComponentDescriptor } from './context';
import WorkspaceComponentTitle from './WorkspaceComponentTitle';
import WorkspaceHeader, { Props as WorkspaceHeaderProps } from './WorkspaceHeader';

export interface Props extends T.Omit<WorkspaceHeaderProps, 'children' | 'onClose'> {
  component: ComponentDescriptor;
  fetchBranchStatus: (branchLike: T.BranchLike, projectKey: string) => Promise<void>;
  height: number;
  onClose: (componentKey: string) => void;
  onLoad: (details: { key: string; name: string; qualifier: string }) => void;
}

export class WorkspaceComponentViewer extends React.PureComponent<Props> {
  container?: HTMLElement | null;

  constructor(props: Props) {
    super(props);
    this.refreshBranchStatus = debounce(this.refreshBranchStatus, 1000);
  }

  componentDidMount() {
    if (document.documentElement) {
      document.documentElement.classList.add('with-workspace');
    }
  }

  componentWillUnmount() {
    if (document.documentElement) {
      document.documentElement.classList.remove('with-workspace');
    }
  }

  handleClose = () => {
    this.props.onClose(this.props.component.key);
  };

  handleIssueChange = (_: T.Issue) => {
    this.refreshBranchStatus();
  };

  handleLoaded = (component: T.SourceViewerFile) => {
    this.props.onLoad({
      key: this.props.component.key,
      name: component.path,
      qualifier: component.q
    });

    if (this.container && this.props.component.line) {
      const row = this.container.querySelector(
        `.source-line[data-line-number="${this.props.component.line}"]`
      );
      if (row) {
        scrollToElement(row, {
          smooth: false,
          parent: this.container,
          topOffset: 50,
          bottomOffset: 50
        });
      }
    }
  };

  refreshBranchStatus = () => {
    const { component } = this.props;
    const { branchLike } = component;
    if (branchLike && (isPullRequest(branchLike) || isShortLivingBranch(branchLike))) {
      getParents(component.key).then(
        (parents?: any[]) => {
          if (parents && parents.length > 0) {
            this.props.fetchBranchStatus(branchLike, parents.pop().key);
          }
        },
        () => {}
      );
    }
  };

  render() {
    const { component } = this.props;

    return (
      <div className="workspace-viewer">
        <WorkspaceHeader
          maximized={this.props.maximized}
          onClose={this.handleClose}
          onCollapse={this.props.onCollapse}
          onMaximize={this.props.onMaximize}
          onMinimize={this.props.onMinimize}
          onResize={this.props.onResize}>
          <WorkspaceComponentTitle component={component} />
        </WorkspaceHeader>

        <div
          className="workspace-viewer-container"
          ref={node => (this.container = node)}
          style={{ height: this.props.height }}>
          <SourceViewer
            aroundLine={component.line}
            branchLike={component.branchLike}
            component={component.key}
            highlightedLine={component.line}
            onIssueChange={this.handleIssueChange}
            onLoaded={this.handleLoaded}
          />
        </div>
      </div>
    );
  }
}

const mapDispatchToProps = { fetchBranchStatus: fetchBranchStatus as any };

export default connect(
  null,
  mapDispatchToProps
)(WorkspaceComponentViewer);
