import $ from 'jquery';

export function getQueue (data) {
  const url = baseUrl + '/api/ce/queue';
  return $.get(url, data);
}

export function getActivity (data) {
  const url = baseUrl + '/api/ce/activity';
  return $.get(url, data);
}

export function getTask (id) {
  const url = baseUrl + '/api/ce/task';
  return $.get(url, { id });
}

export function cancelTask (id) {
  const url = baseUrl + '/api/ce/cancel';
  return $.post(url, { id }).then(getTask.bind(null, id));
}

export function cancelAllTasks () {
  const url = baseUrl + '/api/ce/cancel_all';
  return $.post(url);
}

export function getTasksForComponent(componentId) {
  const url = baseUrl + '/api/ce/component';
  const data = { componentId };
  return new Promise((resolve) => $.get(url, data).done(resolve));
}
