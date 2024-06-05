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
import classNames from 'classnames';
import { IssueMessageHighlighting, LocationMarker, StyledMarker, themeColor } from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../helpers/l10n';
import { MessageFormatting } from '../../types/issues';
import LocationMessage from '../common/LocationMessage';
import './SingleFileLocationNavigator.css';

interface Props {
  concealedMarker?: boolean;
  index: number;
  message: string | undefined;
  messageFormattings?: MessageFormatting[];
  onClick: (index: number) => void;
  selected: boolean;
}

export default class SingleFileLocationNavigator extends React.PureComponent<Props> {
  node?: HTMLElement | null;

  componentDidMount() {
    if (this.props.selected && this.node) {
      this.node.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'center',
      });
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.selected && prevProps.selected !== this.props.selected && this.node) {
      this.node.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'center',
      });
    }
  }

  handleClick = () => {
    this.props.onClick(this.props.index);
  };

  render() {
    const { index, concealedMarker, message, messageFormattings, selected } = this.props;

    return (
      <StyledButton
        onClick={this.handleClick}
        aria-current={selected ? 'location' : false}
        className={classNames('sw-p-1 sw-flex sw-items-center sw-gap-2', {
          selected,
        })}
        ref={(n) => (this.node = n)}
      >
        <LocationMarker selected={selected} text={concealedMarker ? undefined : index + 1} />
        <LocationMessage>
          {message ? (
            <IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />
          ) : (
            translateWithParameters('issue.location_x', index + 1)
          )}
        </LocationMessage>
      </StyledButton>
    );
  }
}

const StyledButton = styled.button`
  color: ${themeColor('pageContent')};
  cursor: pointer;
  outline: none;
  border: none;
  background: transparent;

  &.selected,
  &:hover,
  &:focus {
    background-color: ${themeColor('subnavigationSelected')};
  }

  &:hover ${StyledMarker} {
    background-color: ${themeColor('codeLineLocationMarkerSelected')};
  }
`;
