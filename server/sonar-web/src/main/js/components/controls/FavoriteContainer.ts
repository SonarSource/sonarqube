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
import { connect } from 'react-redux';
import FavoriteBaseStateless from './FavoriteBaseStateless';
import { isFavorite } from '../../store/rootReducer';
import * as actionCreators from '../../store/favorites/duck';
import * as api from '../../api/favorites';
import { addGlobalErrorMessage } from '../../store/globalMessages/duck';
import { parseError } from '../../helpers/request';

const addFavorite = (componentKey: string) => (dispatch: Function) => {
  // optimistic update
  dispatch(actionCreators.addFavorite(componentKey));
  api.addFavorite(componentKey).catch(error => {
    dispatch(actionCreators.removeFavorite(componentKey));
    parseError(error).then(message => dispatch(addGlobalErrorMessage(message)));
  });
};

const removeFavorite = (componentKey: string) => (dispatch: Function) => {
  // optimistic update
  dispatch(actionCreators.removeFavorite(componentKey));
  api.removeFavorite(componentKey).catch(error => {
    dispatch(actionCreators.addFavorite(componentKey));
    parseError(error).then(message => dispatch(addGlobalErrorMessage(message)));
  });
};

const mapStateToProps = (state: any, ownProps: any) => ({
  favorite: isFavorite(state, ownProps.componentKey)
});

const mapDispatchToProps = (dispatch: Function, ownProps: any) => ({
  addFavorite: () => dispatch(addFavorite(ownProps.componentKey)),
  removeFavorite: () => dispatch(removeFavorite(ownProps.componentKey))
});

export default connect(mapStateToProps, mapDispatchToProps)(FavoriteBaseStateless);
