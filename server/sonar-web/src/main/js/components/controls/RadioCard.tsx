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
import * as React from 'react';
import * as classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import RecommendedIcon from '../icons-components/RecommendedIcon';
import { translate } from '../../helpers/l10n';
import './RadioCard.css';

export interface RadioCardProps {
  className?: string;
  disabled?: boolean;
  onClick?: () => void;
  selected?: boolean;
}

interface Props extends RadioCardProps {
  children: React.ReactNode;
  recommended?: string;
  title: React.ReactNode;
  titleInfo?: React.ReactNode;
}

export default function RadioCard(props: Props) {
  const { className, disabled, onClick, recommended, selected, titleInfo } = props;
  const isActionable = Boolean(onClick);
  return (
    <div
      aria-checked={selected}
      className={classNames(
        'radio-card',
        { 'radio-card-actionable': isActionable, disabled, selected },
        className
      )}
      onClick={isActionable && !disabled ? onClick : undefined}
      role="radio"
      tabIndex={0}>
      <h2 className="radio-card-header big-spacer-bottom">
        <span className="display-flex-center">
          {isActionable && (
            <i className={classNames('icon-radio', 'spacer-right', { 'is-checked': selected })} />
          )}
          {props.title}
        </span>
        {titleInfo}
      </h2>
      <div className="radio-card-body">{props.children}</div>
      {recommended && (
        <div className="radio-card-recommended">
          <RecommendedIcon className="spacer-right" />
          <FormattedMessage
            defaultMessage={recommended}
            id={recommended}
            values={{ recommended: <strong>{translate('recommended')}</strong> }}
          />
        </div>
      )}
    </div>
  );
}
