import _ from 'underscore';
import React from 'react';

import { formatMeasure } from '../../../helpers/measures';


const ComponentMeasure = ({ component, metricKey, metricType }) => {
  const measure = _.findWhere(component.msr, { key: metricKey });
  return (
      <span>
        {measure ? formatMeasure(measure.val, metricType) : ''}
      </span>
  );
};


export default ComponentMeasure;
