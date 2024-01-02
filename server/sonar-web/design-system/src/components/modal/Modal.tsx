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
import { Global, css, useTheme } from '@emotion/react';
import classNames from 'classnames';
import { Fragment, ReactNode } from 'react';
import { useIntl } from 'react-intl';
import ReactModal from 'react-modal';
import tw from 'twin.macro';
import { themeColor } from '../../helpers';
import { REACT_DOM_CONTAINER } from '../../helpers/constants';
import { Theme } from '../../types/theme';
import { ButtonSecondary } from '../buttons';
import { ModalBody } from './ModalBody';
import { ModalFooter } from './ModalFooter';
import { ModalHeader } from './ModalHeader';

ReactModal.setAppElement(REACT_DOM_CONTAINER);

/* eslint-disable react/jsx-fragments */

interface CommonProps {
  closeOnOverlayClick?: boolean;
  isLarge?: boolean;
  isOpen?: boolean;
  isOverflowVisible?: boolean;
  isScrollable?: boolean;
  onClose: VoidFunction;
}

interface ChildrenProp {
  children: React.ReactNode;
}

interface NotChildrenProp {
  children?: never;
}

interface SectionsProps {
  body: React.ReactNode;
  headerDescription?: string | ReactNode;
  headerTitle: string | ReactNode;
  loading?: boolean;
  primaryButton?: ReactNode;
  secondaryButtonLabel?: ReactNode;
}

type NotSectionsProps = {
  [prop in keyof SectionsProps]?: never;
};

export type PropsWithChildren = CommonProps & ChildrenProp & NotSectionsProps;

export type PropsWithSections = CommonProps & SectionsProps & NotChildrenProp;

type Props = PropsWithChildren | PropsWithSections;

function hasNoChildren(props: Partial<Props>): props is PropsWithSections {
  return (props as PropsWithChildren).children === undefined;
}

export function Modal({
  closeOnOverlayClick = true,
  isLarge,
  isOpen = true,
  isOverflowVisible = false,
  isScrollable = true,
  onClose,
  ...props
}: Props) {
  const theme = useTheme();
  const intl = useIntl();
  return (
    <Fragment>
      <Global styles={globalStyles({ theme })} />

      <ReactModal
        aria={{ labelledby: 'modal_header_title' }}
        className={classNames('design-system-modal-contents modal', { large: isLarge })}
        isOpen={isOpen}
        onRequestClose={onClose}
        overlayClassName="design-system-modal-overlay"
        shouldCloseOnEsc
        shouldCloseOnOverlayClick={closeOnOverlayClick}
        shouldFocusAfterRender
        shouldReturnFocusAfterClose
      >
        {hasNoChildren(props) ? (
          <Fragment>
            <ModalHeader description={props.headerDescription} title={props.headerTitle} />

            <ModalBody isOverflowVisible={isOverflowVisible} isScrollable={isScrollable}>
              {props.body}
            </ModalBody>

            <ModalFooter
              loading={props.loading}
              primaryButton={props.primaryButton}
              secondaryButton={
                <ButtonSecondary
                  className="js-modal-close sw-capitalize"
                  disabled={props.loading}
                  onClick={onClose}
                  type="reset"
                >
                  {props.secondaryButtonLabel ?? intl.formatMessage({ id: 'close' })}
                </ButtonSecondary>
              }
            />
          </Fragment>
        ) : (
          (props as PropsWithChildren).children
        )}
      </ReactModal>
    </Fragment>
  );
}

const globalStyles = ({ theme }: { theme: Theme }) => css`
  .design-system-modal-contents {
    ${tw`sw-container sw-flex sw-flex-col`}
    ${tw`sw-p-9`}
    ${tw`sw-rounded-2`}
    ${tw`sw-z-modal`}
    ${tw`sw-box-border`}

    background-color: ${themeColor('modalContents')({ theme })};
    max-height: calc(100vh - 200px);
    min-height: 160px;
    width: 544px;

    &.large {
      max-width: 1280px;
      min-width: 1040px;
      transform: translateX(-50%);
      margin-left: 0px;
    }
  }

  .design-system-modal-overlay {
    ${tw`sw-fixed sw-inset-0`}
    ${tw`sw-flex sw-items-center sw-justify-center`}
    ${tw`sw-z-modal-overlay`}

    background-color: ${themeColor('modalOverlay')({ theme })};
  }
`;

Modal.displayName = 'Modal';
Modal.Body = ModalBody;
Modal.Footer = ModalFooter;
Modal.Header = ModalHeader;
