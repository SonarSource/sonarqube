/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import classNames from 'classnames';

export default class FavoriteBase extends React.Component {
  static propTypes = {
    favorite: React.PropTypes.bool.isRequired,
    addFavorite: React.PropTypes.func.isRequired,
    removeFavorite: React.PropTypes.func.isRequired,
    className: React.PropTypes.string
  };

  constructor(props) {
    super(props);
    this.state = { favorite: this.props.favorite };
  }

  componentWillMount() {
    this.mounted = true;
    this.toggleFavorite = this.toggleFavorite.bind(this);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  toggleFavorite(e) {
    e.preventDefault();
    if (this.state.favorite) {
      this.removeFavorite();
    } else {
      this.addFavorite();
    }
  }

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

  renderSVG() {
    /* eslint max-len: 0 */
    return (
      <svg width="16" height="16">
        <path
          d="M15.4275,5.77678C15.4275,5.90773 15.3501,6.05059 15.1953,6.20536L11.9542,9.36608L12.7221,13.8304C12.728,13.872 12.731,13.9316 12.731,14.0089C12.731,14.1339 12.6998,14.2396 12.6373,14.3259C12.5748,14.4122 12.484,14.4554 12.3649,14.4554C12.2518,14.4554 12.1328,14.4197 12.0078,14.3482L7.99888,12.2411L3.98995,14.3482C3.85901,14.4197 3.73996,14.4554 3.63281,14.4554C3.50781,14.4554 3.41406,14.4122 3.35156,14.3259C3.28906,14.2396 3.25781,14.1339 3.25781,14.0089C3.25781,13.9732 3.26377,13.9137 3.27567,13.8304L4.04353,9.36608L0.793531,6.20536C0.644719,6.04464 0.570313,5.90178 0.570313,5.77678C0.570313,5.55654 0.736979,5.41964 1.07031,5.36606L5.55245,4.71428L7.56138,0.651781C7.67447,0.407729 7.8203,0.285703 7.99888,0.285703C8.17745,0.285703 8.32328,0.407729 8.43638,0.651781L10.4453,4.71428L14.9274,5.36606C15.2608,5.41964 15.4274,5.55654 15.4274,5.77678L15.4275,5.77678Z"
        />
      </svg>
    );
  }

  render() {
    const className = classNames(
      'icon-star',
      {
        'icon-star-favorite': this.state.favorite
      },
      this.props.className
    );

    return (
      <a className={className} href="#" onClick={this.toggleFavorite}>
        {this.renderSVG()}
      </a>
    );
  }
}
