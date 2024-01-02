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
/* eslint-disable jsx-a11y/no-static-element-interactions, jsx-a11y/no-noninteractive-tabindex */
import classNames from 'classnames';
import * as React from 'react';
import './Step.css';

interface Props {
  finished?: boolean;
  onOpen?: VoidFunction;
  open: boolean;
  renderForm: () => React.ReactNode;
  renderResult?: () => React.ReactNode;
  stepNumber?: number;
  stepTitle: React.ReactNode;
}

export default function Step(props: Props) {
  const { finished, open, stepNumber, stepTitle } = props;
  const className = classNames('boxed-group', 'onboarding-step', {
    'is-open': open,
    'is-finished': finished,
    'no-step-number': stepNumber === undefined,
  });

  const clickable = !open && finished && props.onOpen !== undefined;

  const handleClick = (event: React.MouseEvent<HTMLDivElement>) => {
    event.preventDefault();
    if (props.onOpen !== undefined) {
      props.onOpen();
    }
  };

  return (
    <div
      className={className}
      onClick={clickable ? handleClick : undefined}
      role={clickable ? 'button' : undefined}
      tabIndex={clickable ? 0 : undefined}
    >
      {stepNumber !== undefined && <div className="onboarding-step-number">{stepNumber}</div>}
      {!open && props.renderResult && props.renderResult()}
      <div className="boxed-group-header">
        <h2>{stepTitle}</h2>
      </div>
      {open ? <div>{props.renderForm()}</div> : <div className="boxed-group-inner" />}
    </div>
  );
}
