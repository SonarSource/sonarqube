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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import FavoriteContainer from '../../../components/controls/FavoriteContainer';
import { getComponentForSourceViewer } from '../../../api/components';
import { receiveFavorites } from '../../../store/favorites/duck';

/*:: type FavComponent = { key: string, canMarkAsFavorite: boolean, fav: boolean }; */

/*:: type Props = {
  className?: string,
  component: string,
  onReceiveComponent: (component: FavComponent) => void
}; */

/*:: type State = { component: ?FavComponent }; */

class MeasureFavoriteContainer extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    component: null
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponentFavorite(this.props);
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.component !== this.props.component) {
      this.fetchComponentFavorite(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponentFavorite({ component, onReceiveComponent } /*: Props */) {
    getComponentForSourceViewer(component).then(component => {
      this.setState({ component });
      onReceiveComponent(component);
    });
  }

  render() {
    const { component } = this.state;
    if (component == null || !component.canMarkAsFavorite) {
      return null;
    }
    return (
      <FavoriteContainer className={this.props.className} componentKey={this.props.component} />
    );
  }
}

const mapStateToProps = null;

const mapDispatchToProps = {
  onReceiveComponent: (component /*: FavComponent */) => dispatch => {
    if (component.canMarkAsFavorite) {
      const favorites = [];
      const notFavorites = [];
      if (component.fav) {
        favorites.push({ key: component.key });
      } else {
        notFavorites.push({ key: component.key });
      }
      dispatch(receiveFavorites(favorites, notFavorites));
    }
  }
};

export default connect(mapStateToProps, mapDispatchToProps)(MeasureFavoriteContainer);
