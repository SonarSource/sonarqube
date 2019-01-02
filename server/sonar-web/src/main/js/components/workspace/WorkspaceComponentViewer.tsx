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
import { ComponentDescriptor } from './context';
import WorkspaceHeader, { Props as WorkspaceHeaderProps } from './WorkspaceHeader';
import WorkspaceComponentTitle from './WorkspaceComponentTitle';
import SourceViewer from '../SourceViewer/SourceViewer';
import { scrollToElement } from '../../helpers/scrolling';

export interface Props extends T.Omit<WorkspaceHeaderProps, 'children' | 'onClose'> {
  component: ComponentDescriptor;
  height: number;
  onClose: (componentKey: string) => void;
  onLoad: (details: { key: string; name: string; qualifier: string }) => void;
}

export default class WorkspaceComponentViewer extends React.PureComponent<Props> {
  container?: HTMLElement | null;

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
            onLoaded={this.handleLoaded}
          />
        </div>
      </div>
    );
  }
}
