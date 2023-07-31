import classNames from 'classnames';
import { Link, Pill } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../helpers/docs';
import { translate } from '../../helpers/l10n';
import { CleanCodeAttributeCategory } from '../../types/issues';
import Tooltip from '../controls/Tooltip';

export interface Props {
  className?: string;
  cleanCodeAttributeCategory: CleanCodeAttributeCategory;
}

export function CleanCodeAttributePill(props: Props) {
  const { className, cleanCodeAttributeCategory } = props;

  const docUrl = useDocUrl('/');

  return (
    <Tooltip
      overlay={
        <>
          <p className="sw-mb-4">
            {translate('issue.clean_code_attribute_category', cleanCodeAttributeCategory, 'title')}
          </p>
          <p>
            {translate('issue.clean_code_attribute_category', cleanCodeAttributeCategory, 'advice')}
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
      <span className="sw-w-fit">
        <Pill variant="neutral" className={classNames('sw-mr-2', className)}>
          {translate('issue.clean_code_attribute_category', cleanCodeAttributeCategory, 'issue')}
        </Pill>
      </span>
    </Tooltip>
  );
}
