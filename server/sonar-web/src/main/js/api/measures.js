import { getJSON } from '../helpers/request.js';

export function getMeasures (componentKey, metrics) {
  let url = baseUrl + '/api/resources/index';
  let data = { resource: componentKey, metrics: metrics.join(',') };
  return getJSON(url, data).then(r => {
    let msr = r[0].msr || [];
    let measures = {};
    msr.forEach(measure => {
      measures[measure.key] = measure.val || measure.data;
    });
    return measures;
  });
}
