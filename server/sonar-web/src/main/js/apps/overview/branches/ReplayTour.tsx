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
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';

interface Props {
  closeTour: () => void;
  run: boolean;
  tourCompleted: boolean;
}

export default function ReplayTourGuide({ run, closeTour, tourCompleted }: Readonly<Props>) {
  const onToggle = ({ action }: { action: string }) => {
    if (action === 'skip' || action === 'close') {
      closeTour();
    }
  };

  const constructContent = (first: string) => <p className="sw-mt-2">{translate(first)}</p>;

  const docUrl = useDocUrl('improving/clean-as-you-code/');

  const steps: SpotlightTourStep[] = [
    {
      disableOverlayClose: true,
      target: '[data-spotlight-id="take-tour-1"]',
      content: constructContent('guiding.replay_tour_button.1.content'),
      title: tourCompleted
        ? translate('guiding.replay_tour_button.tour_completed.1.title')
        : translate('guiding.replay_tour_button.1.title'),
      placement: 'left',
    },
  ];

  return (
    <div>
      <SpotlightTour
        actionLabel={tourCompleted ? translate('learn_more.clean_code') : undefined}
        actionPath={tourCompleted ? docUrl : undefined}
        backLabel={translate('go_back')}
        callback={onToggle}
        closeLabel={translate('got_it')}
        continuous
        disableOverlay
        nextLabel={translate('next')}
        run={run}
        skipLabel={translate('skip')}
        stepXofYLabel={() => ''}
        steps={steps}
        width={350}
      />
    </div>
  );
}
