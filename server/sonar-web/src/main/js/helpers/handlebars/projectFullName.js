module.exports = function (component) {
  return component.projectName + (component.subProjectName ? (' / ' + component.subProjectName) : '');
};
