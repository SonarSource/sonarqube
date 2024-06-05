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
import { FavoriteButton } from 'design-system';
import * as React from 'react';
import { addFavorite, removeFavorite } from '../../api/favorites';
import { translate, translateWithParameters } from '../../helpers/l10n';
import Tooltip from './Tooltip';

interface Props {
  className?: string;
  component: string;
  componentName?: string;
  favorite: boolean;
  handleFavorite?: (component: string, isFavorite: boolean) => void;
  qualifier: string;
}

interface State {
  favorite: boolean;
}

export default class Favorite extends React.PureComponent<Props, State> {
  mounted = false;
  buttonNode?: HTMLElement | null;

  constructor(props: Props) {
    super(props);

    this.state = {
      favorite: props.favorite,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(_prevProps: Props, prevState: State) {
    if (prevState.favorite !== this.props.favorite) {
      this.setState({ favorite: this.props.favorite });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  toggleFavorite = () => {
    const newFavorite = !this.state.favorite;
    const apiMethod = newFavorite ? addFavorite : removeFavorite;

    return apiMethod(this.props.component).then(() => {
      if (this.mounted) {
        this.setState({ favorite: newFavorite }, () => {
          if (this.props.handleFavorite) {
            this.props.handleFavorite(this.props.component, newFavorite);
          }
          if (this.buttonNode) {
            this.buttonNode.focus();
          }
        });
      }
    });
  };

  render() {
    const { className, componentName, qualifier } = this.props;
    const { favorite } = this.state;

    const actionName = favorite ? 'remove' : 'add';
    const overlay = componentName
      ? translateWithParameters(`favorite.action.${qualifier}.${actionName}_x`, componentName)
      : translate('favorite.action', qualifier, actionName);

    return (
      <FavoriteButton
        className={className}
        overlay={overlay}
        toggleFavorite={this.toggleFavorite}
        tooltip={Tooltip}
        favorite={favorite}
        innerRef={(node: HTMLElement | null) => (this.buttonNode = node)}
      />
    );
  }
}
