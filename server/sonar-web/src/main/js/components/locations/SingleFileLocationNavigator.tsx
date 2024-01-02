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
import classNames from 'classnames';
import * as React from 'react';
import { MessageFormatting } from '../../types/issues';
import LocationIndex from '../common/LocationIndex';
import LocationMessage from '../common/LocationMessage';
import { ButtonPlain } from '../controls/buttons';
import { IssueMessageHighlighting } from '../issue/IssueMessageHighlighting';
import './SingleFileLocationNavigator.css';

interface Props {
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
    const { index, message, messageFormattings, selected } = this.props;

    return (
      <ButtonPlain
        preventDefault={true}
        stopPropagation={true}
        aria-current={selected ? 'location' : false}
        className={classNames('locations-navigator', { selected })}
        innerRef={(node) => {
          this.node = node;
        }}
        onClick={this.handleClick}
      >
        <LocationIndex>{index + 1}</LocationIndex>
        <LocationMessage>
          {<IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />}
        </LocationMessage>
      </ButtonPlain>
    );
  }
}
