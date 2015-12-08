import { getJSON } from '../helpers/request.js';


export function getMeasures (componentKey, metrics) {
  const url = baseUrl + '/api/resources/index';
  const data = { resource: componentKey, metrics: metrics.join(',') };
  return getJSON(url, data).then(r => {
    const msr = r[0].msr || [];
    const measures = {};
    msr.forEach(measure => {
      measures[measure.key] = measure.val || measure.data;
    });
    return measures;
  });
}


export function getMeasuresAndVariations (componentKey, metrics) {
  const url = baseUrl + '/api/resources/index';
  const data = { resource: componentKey, metrics: metrics.join(','), includetrends: 'true' };
  return getJSON(url, data).then(r => {
    const msr = r[0].msr || [];
    const measures = {};
    msr.forEach(measure => {
      measures[measure.key] = {
        value: measure.val != null ? measure.val : measure.data,
        var1: measure.var1,
        var2: measure.var2,
        var3: measure.var3
      };
    });
    return measures;
  });
}
