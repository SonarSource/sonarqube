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
import classNames from 'classnames';

/*::
type Props = {|
  finished: boolean,
  onOpen: () => void,
  open: boolean,
  renderForm: () => React.Element<*>,
  renderResult: () => ?React.Element<*>,
  stepNumber: number,
  stepTitle: string
|};
*/

export default function Step(props /*: Props */) {
  const className = classNames('boxed-group', 'onboarding-step', {
    'is-open': props.open,
    'is-finished': props.finished
  });

  const clickable = !props.open && props.finished;

  const handleClick = (event /*: Event */) => {
    event.preventDefault();
    props.onOpen();
  };

  return (
    <div
      className={className}
      onClick={clickable ? handleClick : undefined}
      role={clickable ? 'button' : undefined}
      tabIndex={clickable ? 0 : undefined}>
      <div className="onboarding-step-number">{props.stepNumber}</div>
      {!props.open && props.renderResult()}
      <div className="boxed-group-header">
        <h2>{props.stepTitle}</h2>
      </div>
      {props.open ? props.renderForm() : <div className="boxed-group-inner" />}
    </div>
  );
}
