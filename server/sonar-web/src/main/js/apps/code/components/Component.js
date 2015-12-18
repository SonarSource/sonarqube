import React from 'react';

import ComponentName from './ComponentName';
import ComponentMeasure from './ComponentMeasure';
import ComponentDetach from './ComponentDetach';


const Component = ({ component, coverageMetric, onBrowse }) => (
    <tr>
      <td className="thin nowrap">
        <span className="spacer-right">
          <ComponentDetach component={component}/>
        </span>
      </td>
      <td className="code-name-cell">
        <ComponentName
            component={component}
            onBrowse={onBrowse}/>
      </td>
      <td className="thin nowrap text-right">
        <div className="code-components-cell">
          <ComponentMeasure
              component={component}
              metricKey="ncloc"
              metricType="SHORT_INT"/>
        </div>
      </td>
      <td className="thin nowrap text-right">
        <div className="code-components-cell">
          <ComponentMeasure
              component={component}
              metricKey="sqale_index"
              metricType="SHORT_WORK_DUR"/>
        </div>
      </td>
      <td className="thin nowrap text-right">
        <div className="code-components-cell">
          <ComponentMeasure
              component={component}
              metricKey="violations"
              metricType="SHORT_INT"/>
        </div>
      </td>
      <td className="thin nowrap text-right">
        <div className="code-components-cell">
          <ComponentMeasure
              component={component}
              metricKey={coverageMetric}
              metricType="PERCENT"/>
        </div>
      </td>
      <td className="thin nowrap text-right">
        <div className="code-components-cell">
          <ComponentMeasure
              component={component}
              metricKey="duplicated_lines_density"
              metricType="PERCENT"/>
        </div>
      </td>
    </tr>
);


export default Component;
