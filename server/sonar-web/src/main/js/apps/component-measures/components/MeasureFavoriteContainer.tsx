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
import Favorite from '../../../components/controls/Favorite';
import { getComponentForSourceViewer } from '../../../api/components';
import { isMainBranch } from '../../../helpers/branches';
import { BranchLike, SourceViewerFile } from '../../../app/types';

type FavComponent = Pick<SourceViewerFile, 'canMarkAsFavorite' | 'fav' | 'key' | 'q'>;

interface Props {
  branchLike?: BranchLike;
  className?: string;
  component: string;
}

interface State {
  component?: FavComponent;
}

export default class MeasureFavoriteContainer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchComponentFavorite();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.component !== this.props.component) {
      this.fetchComponentFavorite();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponentFavorite() {
    getComponentForSourceViewer({ component: this.props.component }).then(
      component => {
        if (this.mounted) {
          this.setState({ component });
        }
      },
      () => {}
    );
  }

  render() {
    const { component } = this.state;
    if (
      !component ||
      !component.canMarkAsFavorite ||
      (this.props.branchLike && !isMainBranch(this.props.branchLike))
    ) {
      return null;
    }
    return (
      <Favorite
        className={this.props.className}
        component={component.key}
        favorite={component.fav || false}
        qualifier={component.q}
      />
    );
  }
}
