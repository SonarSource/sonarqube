import $ from 'jquery';

export function getQueue (data) {
  let url = baseUrl + '/api/ce/queue';
  return $.get(url, data);
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

export function cancelAllTasks () {
  let url = baseUrl + '/api/ce/cancel_all';
  return $.post(url);
}

export function getTasksForComponent(componentId) {
  let url = baseUrl + '/api/ce/component';
  let data = { componentId };
  return new Promise((resolve) => $.get(url, data).done(resolve));
}
