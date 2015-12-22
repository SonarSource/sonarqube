import React from 'react';

import Component from './Component';
import ComponentsEmpty from './ComponentsEmpty';


const Components = ({ baseComponent, components, coverageMetric, onBrowse }) => (
    <table className="data zebra">
      <thead>
        <tr>
          <th className="thin nowrap">&nbsp;</th>
          <th>&nbsp;</th>
          <th className="thin nowrap text-right">{window.t('metric.ncloc.name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.sqale_index.short_name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.violations.name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.coverage.name')}</th>
          <th className="thin nowrap text-right">{window.t('metric.duplicated_lines_density.short_name')}</th>
        </tr>
      </thead>
      {baseComponent && (
          <tbody>
            <Component
                key={baseComponent.key}
                component={baseComponent}
                coverageMetric={coverageMetric}/>
            <tr className="blank">
              <td colSpan="7">&nbsp;</td>
            </tr>
          </tbody>
      )}
      <tbody>
        {components.length ? (
            components.map(component => (
                <Component
                    key={component.key}
                    component={component}
                    coverageMetric={coverageMetric}
                    onBrowse={onBrowse}/>
            ))
        ) : (
            <ComponentsEmpty/>
        )}
      </tbody>
    </table>
);


export default Components;
