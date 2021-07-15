/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';

type EllipsisPredicate = (
  node: HTMLElement,
  props: Omit<AutoEllipsisProps, 'customShouldEllipsis'>
) => boolean;

interface AutoEllipsisProps {
  customShouldEllipsis?: EllipsisPredicate;
  maxHeight?: number;
  maxWidth?: number;
  useParent?: boolean;
}

interface Props extends AutoEllipsisProps {
  children: React.ReactElement;
}

/*
 * This component allows to automatically add the .text-ellipsis class on it's children if this one
 * might overflow the max width/height passed as props.
 * If one of maxHeight or maxWidth is not specified, they will be ignored in the conditions to add the ellipsis class.
 * If useParent is true, then the parent size will be used instead of the undefined maxHeight/maxWidth
 */
export default function AutoEllipsis(props: Props) {
  const { children, ...autoEllipsisProps } = props;
  const [autoEllispis, ref] = useAutoEllipsis(autoEllipsisProps);

  return React.cloneElement(children, {
    className: classNames(children.props.className, { 'text-ellipsis': autoEllispis }),
    ref,
  });
}

export function useAutoEllipsis(props: AutoEllipsisProps): [boolean, (node: HTMLElement) => void] {
  const [autoEllipsis, setAutoEllipsis] = React.useState(false);

  // useCallback instead of useRef to be able to compute if the flag is needed as soon as the ref is attached
  // useRef doesn't accept a callback to notify us that the current ref value was attached,
  // see https://reactjs.org/docs/hooks-faq.html#how-can-i-measure-a-dom-node for more info on this.
  const ref = React.useCallback(
    (node: HTMLElement) => {
      if (!autoEllipsis && node) {
        const shouldEllipsis = props.customShouldEllipsis ?? defaultShouldEllipsis;
        setAutoEllipsis(shouldEllipsis(node, props));
      }
    },
    // We don't want to apply this effect when ellipsis state change, only this effect can change it
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [props.customShouldEllipsis, props.maxHeight, props.maxWidth, props.useParent]
  );

  return [autoEllipsis, ref];
}

export const defaultShouldEllipsis: EllipsisPredicate = (
  node,
  { useParent = true, maxWidth, maxHeight }
) => {
  if (node.parentElement && useParent) {
    maxWidth = maxWidth ?? node.parentElement.clientWidth;
    maxHeight = maxHeight ?? node.parentElement.clientHeight;
  }
  return (
    (maxWidth !== undefined && node.clientWidth > maxWidth) ||
    (maxHeight !== undefined && node.clientHeight > maxHeight)
  );
};
