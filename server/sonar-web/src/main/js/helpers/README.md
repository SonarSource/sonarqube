## Making Requests

Since 6.0 all API requests must provide CSRF protection token. In order to
help you to do so, we provide a set of useful helpers.

### Getting Started
Let's start with a simple GET request.

```
window.sonarqube.appStarted.then(function () {
  // some code here
  window.SonarRequest
      .getJSON('/api/issues/search')
      .then(function (response) {
        // here 'response' contains the object representing the JSON output
      });
});
```

`window.sonarqube.appStarted` returns a `Promise` telling you when the
application is started and all the runtime libraries and hooks are installed.

`window.SonarRequest` contains all the helper methods to do an API requests.

`window.SonarRequest.getJSON` is a simplest helper to do an API call,
receive some data and parse it as JSON.

### API Documentation

#### `window.sonarqube.appStarted: Promise`
A promise telling when the application has started.
You must put your code in the `resolve` path.

#### `window.SonarRequest.request(url: string): Request`
Start making an API call.
Return a `Request` instance which has the following methods:
* `setMethod(method: string): Request` sets the http method, can be `GET`, `POST`, etc.
* `setData(data: object): Request` sets the request parameters`
* `submit(): Promise` sends the request

#### `window.SonarRequest.getJSON(url: string[, data: object]): Promise`
Send a GET request, get a response, parse it as JSON.

#### `window.SonarRequest.postJSON(url: string[, data: object]): Promise`
Send a POST request, get a response, parse it as JSON.

#### `window.SonarRequest.post(url: string[, data: object]): Promise`
Send a POST request, ignore the response content.

### Important to Know

Under the hood `Request` uses `fetch API`.

All `getJSON`, `postJSON` and `post` check the response status code.
They reject the Promise if the status is not in [200, 300).

### More Examples

#### Get the list of unresolved issues
```
window.SonarRequest.getJSON(
  '/api/issues/search',
  { resolved: false }
).then(function (response) {
  // response.issues contains the list of issues
});
```

#### Create new project
```
window.SonarRequest.post(
  '/api/projects/create',
  { key: 'sample', name: 'Sample' }
).then(function () {
  // the project has been created
});
```

#### Handle bad requests
```
window.SonarRequest.post(
  '/api/users/deactivate',
  { login: 'admin' }
).catch(function (error) {
  // error.response.status === 400
  // error.response.statusText === 'Bad Request'
  // To read the response:
  // error.response.json().then(function (jsonResponse) { ... });
});
```
