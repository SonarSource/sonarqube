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
import { Dispatch } from 'redux';
import { connect, Omit } from 'react-redux';
import { Props } from './SourceViewerBase';
import { lazyLoad } from '../lazyLoad';
import { SourceViewerFile } from '../../app/types';
import { receiveFavorites } from '../../store/favorites/duck';

const mapStateToProps = null;

interface DispatchProps {
  onReceiveComponent: (component: SourceViewerFile) => void;
}

const onReceiveComponent = (component: SourceViewerFile) => (dispatch: Dispatch<any>) => {
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
};

const mapDispatchToProps: DispatchProps = { onReceiveComponent };

type OwnProps = Omit<Props, keyof DispatchProps>;

export default connect<null, DispatchProps, OwnProps>(mapStateToProps, mapDispatchToProps)(
  lazyLoad(() => import(/* webpackPrefetch: true */ './SourceViewerBase'))
);
