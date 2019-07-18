/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import GenericAvatar from 'sonar-ui-common/components/ui/GenericAvatar';
import { getGlobalSettingValue, Store } from '../../store/rootReducer';

interface Props {
  className?: string;
  enableGravatar: boolean;
  gravatarServerUrl: string;
  hash?: string;
  name?: string;
  size: number;
}

function Avatar(props: Props) {
  if (!props.enableGravatar || !props.hash) {
    if (!props.name) {
      return null;
    }
    return <GenericAvatar className={props.className} name={props.name} size={props.size} />;
  }

  const url = props.gravatarServerUrl
    .replace('{EMAIL_MD5}', props.hash)
    .replace('{SIZE}', String(props.size * 2));

  return (
    <img
      alt={props.name}
      className={classNames(props.className, 'rounded')}
      height={props.size}
      src={url}
      width={props.size}
    />
  );
}

const mapStateToProps = (state: Store) => {
  const enableGravatar = getGlobalSettingValue(state, 'sonar.lf.enableGravatar');
  const gravatarServerUrl = getGlobalSettingValue(state, 'sonar.lf.gravatarServerUrl');
  return {
    enableGravatar: Boolean(enableGravatar && enableGravatar.value === 'true'),
    gravatarServerUrl: (gravatarServerUrl && gravatarServerUrl.value) || ''
  };
};

export default connect(mapStateToProps)(Avatar);

export const unconnectedAvatar = Avatar;
