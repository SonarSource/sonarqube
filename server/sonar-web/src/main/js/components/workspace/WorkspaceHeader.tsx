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
import styled from '@emotion/styled';
import {
  CloseIcon,
  CollapseIcon,
  ExpandIcon,
  IconProps,
  InteractiveIcon,
  MinimizeIcon,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { DraggableCore, DraggableData } from 'react-draggable';
import { translate } from '../../helpers/l10n';
import Tooltip from '../controls/Tooltip';

export interface Props {
  children: React.ReactNode;
  maximized?: boolean;
  onClose: () => void;
  onCollapse: () => void;
  onMaximize: () => void;
  onMinimize: () => void;
  onResize: (deltaY: number) => void;
}

export default class WorkspaceHeader extends React.PureComponent<Props> {
  handleDrag = (_event: MouseEvent, data: DraggableData) => {
    this.props.onResize(data.deltaY);
  };

  render() {
    return (
      <StyledWorkSpaceHeader>
        <StyledWorkspaceName className="sw-body-sm sw-inline-flex sw-items-center">
          {this.props.children}
        </StyledWorkspaceName>

        <DraggableCore offsetParent={document.body} onDrag={this.handleDrag}>
          <StyledWorkspaceResizer />
        </DraggableCore>

        <div className="it__workspace-viewer-actions sw-flex sw-gap-1">
          <WorkspaceHeaderButton
            icon={MinimizeIcon}
            onClick={this.props.onCollapse}
            tooltipContent="workspace.minimize"
          />

          {this.props.maximized ? (
            <WorkspaceHeaderButton
              icon={CollapseIcon}
              onClick={this.props.onMinimize}
              tooltipContent="workspace.normal_size"
            />
          ) : (
            <WorkspaceHeaderButton
              icon={ExpandIcon}
              onClick={this.props.onMaximize}
              tooltipContent="workspace.full_window"
            />
          )}

          <WorkspaceHeaderButton
            icon={CloseIcon}
            onClick={this.props.onClose}
            tooltipContent="workspace.close"
          />
        </div>
      </StyledWorkSpaceHeader>
    );
  }
}

const StyledWorkSpaceHeader = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  height: 1.875rem;
  padding: 3px 10px;
  font-weight: 300;
  background-color: ${themeColor('workSpaceNavItemBackground')};
  color: ${themeColor('workSpaceNavItem')};
`;

const StyledWorkspaceName = styled.h6`
  color: ${themeColor('workSpaceNavItem')};
`;

const StyledWorkspaceResizer = styled.div`
  position: absolute;
  top: 3px;
  left: 50%;
  width: 30px;
  height: 5px;
  margin-left: -15px;
  background-image: url(data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzBweCIgaGVpZ2h0PSI1cHgiIHZlcnNpb249IjEuMSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayIgeG1sOnNwYWNlPSJwcmVzZXJ2ZSIgc3R5bGU9ImZpbGwtcnVsZTpldmVub2RkO2NsaXAtcnVsZTpldmVub2RkO3N0cm9rZS1saW5lam9pbjpyb3VuZDtzdHJva2UtbWl0ZXJsaW1pdDoxLjQxNDIxOyI+PGc+PGcgaWQ9IkxheWVyMSI+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSwwLDApIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsNCwwKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDgsMCkiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSwxMiwwKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDE2LDApIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsMjAsMCkiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSwyNCwwKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDI4LDApIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsMCwzKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDQsMykiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSw4LDMpIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsMTIsMykiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSwxNiwzKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDIwLDMpIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsMjQsMykiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSwyOCwzKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDAsMCkiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSw0LDApIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsOCwwKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDEyLDApIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsMTYsMCkiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PGcgdHJhbnNmb3JtPSJtYXRyaXgoMSwwLDAsMSwyMCwwKSI+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgc3R5bGU9ImZpbGw6Izc3NztmaWxsLW9wYWNpdHk6MC41OyIvPjwvZz48ZyB0cmFuc2Zvcm09Im1hdHJpeCgxLDAsMCwxLDI0LDApIj48cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMiIgaGVpZ2h0PSIyIiBzdHlsZT0iZmlsbDojNzc3O2ZpbGwtb3BhY2l0eTowLjU7Ii8+PC9nPjxnIHRyYW5zZm9ybT0ibWF0cml4KDEsMCwwLDEsMjgsMCkiPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIHN0eWxlPSJmaWxsOiM3Nzc7ZmlsbC1vcGFjaXR5OjAuNTsiLz48L2c+PC9nPjwvZz48L3N2Zz4=);
  cursor: ns-resize;
`;

interface WorkspaceHeaderButtonProps {
  icon: React.ComponentType<React.PropsWithChildren<IconProps>>;
  onClick: () => void;
  tooltipContent: string;
}

function WorkspaceHeaderButton({
  icon,
  onClick,
  tooltipContent,
}: Readonly<WorkspaceHeaderButtonProps>) {
  return (
    <Tooltip content={translate(tooltipContent)}>
      <InteractiveIcon
        aria-label={translate(tooltipContent)}
        Icon={icon}
        currentColor
        onClick={onClick}
        size="small"
      />
    </Tooltip>
  );
}
