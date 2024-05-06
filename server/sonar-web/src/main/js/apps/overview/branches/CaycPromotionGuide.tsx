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
import { SpotlightTour, SpotlightTourStep } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  closeTour: (action: string) => void;
  run: boolean;
}

function CaycPromotionGuide(props: Readonly<Props>) {
  const { run } = props;
  const onToggle = ({ action, type }: { action: string; type: string }) => {
    if (type === 'tour:end' && (action === 'close' || action === 'skip')) {
      props.closeTour(action);
    }
  };

  const constructContent = (first: string) => <p className="sw-mt-2">{translate(first)}</p>;

  const constructContentLastStep = (first: string, second: string, third: string) => (
    <>
      <p className="sw-mt-2">
        <FormattedMessage
          defaultMessage={translate(first)}
          id={first}
          values={{
            value: <strong>{translate('ide')}</strong>,
          }}
        />
      </p>
      <p className="sw-mt-2">
        <FormattedMessage
          defaultMessage={translate(second)}
          id={second}
          values={{
            value: <strong>{translate('pull_request.small')}</strong>,
          }}
        />
      </p>
      <p className="sw-mt-2">
        <FormattedMessage
          defaultMessage={translate(third)}
          id={third}
          values={{
            value: <strong>{translate('branch.small')}</strong>,
          }}
        />
      </p>
    </>
  );

  const steps: SpotlightTourStep[] = [
    {
      disableScrolling: false,
      disableOverlayClose: true,
      target: '[data-spotlight-id="cayc-promotion-1"]',
      content: constructContent('guiding.cayc_promotion.1.content.1'),
      title: translate('guiding.cayc_promotion.1.title'),
      placement: 'left',
    },
    {
      disableScrolling: true,
      disableOverlayClose: true,
      target: '[data-spotlight-id="cayc-promotion-2"]',
      content: constructContent('guiding.cayc_promotion.2.content.1'),
      title: translate('guiding.cayc_promotion.2.title'),
      placement: 'left',
    },
    {
      disableScrolling: true,
      disableOverlayClose: true,
      target: '[data-spotlight-id="cayc-promotion-3"]',
      content: constructContent('guiding.cayc_promotion.3.content.1'),
      title: translate('guiding.cayc_promotion.3.title'),
      placement: 'right',
    },
    {
      disableScrolling: true,
      disableOverlayClose: true,
      target: '[data-spotlight-id="cayc-promotion-4"]',
      content: constructContentLastStep(
        'guiding.cayc_promotion.4.content.1',
        'guiding.cayc_promotion.4.content.2',
        'guiding.cayc_promotion.4.content.3',
      ),
      title: translate('guiding.cayc_promotion.4.title'),
      placement: 'right',
      spotlightPadding: 0,
    },
  ];

  return (
    <SpotlightTour
      disableOverlay={false}
      disableScrolling
      backLabel={translate('previous')}
      callback={onToggle}
      closeLabel={translate('complete')}
      continuous
      nextLabel={translate('next')}
      run={run}
      skipLabel={translate('skip')}
      stepXofYLabel={(x: number, y: number) => translateWithParameters('guiding.step_x_of_y', x, y)}
      steps={steps}
      styles={{
        options: {
          zIndex: 1000,
        },
      }}
    />
  );
}

export default CaycPromotionGuide;
