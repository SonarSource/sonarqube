import $ from 'jquery';

export function getQueue () {
  let url = baseUrl + '/api/ce/queue';
  return $.get(url);
}

export function getActivity (data) {
  let url = baseUrl + '/api/ce/activity';
  return $.get(url, data);
}

export function getTask (id) {
  let url = baseUrl + '/api/ce/task';
  return $.get(url, { id });
}

export function cancelTask (id) {
  let url = baseUrl + '/api/ce/cancel';
  return $.post(url, { id }).then(getTask.bind(null, id));
}
