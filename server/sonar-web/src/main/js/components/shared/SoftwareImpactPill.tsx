import classNames from 'classnames';
import { Link, Pill } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../helpers/docs';
import { translate } from '../../helpers/l10n';
import {
  CleanCodeAttributeCategory,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../types/issues';
import Tooltip from '../controls/Tooltip';
import SoftwareImpactSeverityIcon from '../icons/SoftwareImpactSeverityIcon';

export interface Props {
  className?: string;
  cleanCodeAttributeCategory: CleanCodeAttributeCategory;
  severity: SoftwareImpactSeverity;
  quality: SoftwareQuality;
}

export default function SoftwareImpactPill(props: Props) {
  const { cleanCodeAttributeCategory, className, severity, quality } = props;

  const docUrl = useDocUrl('/');

  const variant = {
    [SoftwareImpactSeverity.High]: 'danger',
    [SoftwareImpactSeverity.Medium]: 'warning',
    [SoftwareImpactSeverity.Low]: 'info',
  }[severity] as 'danger' | 'warning' | 'info';

  return (
    <div>
      <Tooltip
        overlay={
          <>
            <p className="sw-mb-4">
              {translate(
                'issue.clean_code_attribute_category',
                cleanCodeAttributeCategory,
                'title'
              )}
            </p>
            <p>
              <FormattedMessage
                id="issue.impact.severity.tooltip"
                defaultMessage={translate('issue.impact.severity.tooltip')}
                values={{
                  severity: translate('severity', severity).toLowerCase(),
                  quality: translate('issue.software_quality', quality).toLowerCase(),
                }}
              />
            </p>
            <hr className="sw-w-full sw-mx-0 sw-my-4" />
            <FormattedMessage
              defaultMessage={translate('learn_more_x')}
              id="learn_more_x"
              values={{
                link: (
                  <Link isExternal to={docUrl}>
                    {translate('issue.type.deprecation.documentation')}
                  </Link>
                ),
              }}
            />
          </>
        }
      >
        <span>
          <Pill
            className={classNames('sw-flex sw-gap-1 sw-items-center', className)}
            variant={variant}
          >
            {translate('issue.software_quality', quality)}
            <SoftwareImpactSeverityIcon severity={severity} />
          </Pill>
        </span>
      </Tooltip>
    </div>
  );
}
