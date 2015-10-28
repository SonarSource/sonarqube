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


export function getMeasuresAndVariations (componentKey, metrics) {
  let url = baseUrl + '/api/resources/index';
  let data = { resource: componentKey, metrics: metrics.join(','), includetrends: 'true' };
  return getJSON(url, data).then(r => {
    let msr = r[0].msr || [];
    let measures = {};
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
