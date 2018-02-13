/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as classNames from 'classnames';
import Tooltip from './Tooltip';
import FavoriteIcon from '../icons-components/FavoriteIcon';
import { translate } from '../../helpers/l10n';

export interface Props {
  addFavorite: () => Promise<void>;
  className?: string;
  favorite: boolean;
  qualifier: string;
  removeFavorite: () => Promise<void>;
}

interface State {
  favorite: boolean;
}

export default class FavoriteBase extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { favorite: this.props.favorite };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.favorite !== this.props.favorite && nextProps.favorite !== this.state.favorite) {
      this.setState({ favorite: nextProps.favorite });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  toggleFavorite = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    if (this.state.favorite) {
      this.removeFavorite();
    } else {
      this.addFavorite();
    }
  };

  addFavorite() {
    this.props.addFavorite().then(() => {
      if (this.mounted) {
        this.setState({ favorite: true });
      }
    });
  }

  removeFavorite() {
    this.props.removeFavorite().then(() => {
      if (this.mounted) {
        this.setState({ favorite: false });
      }
    });
  }

  render() {
    const tooltip = this.state.favorite
      ? translate('favorite.current', this.props.qualifier)
      : translate('favorite.check', this.props.qualifier);
    return (
      <Tooltip overlay={tooltip} placement="left">
        <a
          className={classNames('display-inline-block', 'link-no-underline', this.props.className)}
          href="#"
          onClick={this.toggleFavorite}>
          <FavoriteIcon favorite={this.state.favorite} />
        </a>
      </Tooltip>
    );
  }
}
