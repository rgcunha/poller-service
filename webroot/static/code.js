const listContainer = document.querySelector('#service-list');
const servicesRequest = new Request('/service');

function createList() {
  fetch(servicesRequest)
    .then(response => response.json())
    .then((serviceList) => {
      serviceList.forEach(service => {
        const li = createListItem(service);
        listContainer.appendChild(li);
      });
    });
  };

function createListItem(service) {
  const li = document.createElement("li");
  li.setAttribute("id", service.id);
  li.setAttribute("data-name", service.name);
  li.setAttribute("data-url", service.url);

  const anchor = createAnchor(service.url, service.name);
  const deleteBtn = createBtn('Delete', onDelete);
  const statusIcon = createStatusIcon(service.status);

  li.appendChild(statusIcon);
  li.appendChild(anchor);
  li.appendChild(deleteBtn);

  return li;
}

function createBtn(value, onClick) {
  const btn = document.createElement("input");
  btn.setAttribute("value", value);
  btn.setAttribute("type", "button");
  btn.onclick = onClick;
  return btn;
}

function createAnchor(href, text) {
  const anchor = document.createElement("a");
  const textNode = document.createTextNode(text);
  anchor.setAttribute("href", href);
  anchor.setAttribute("target", "_blank");
  anchor.appendChild(textNode);
  return anchor;
}

function createStatusIcon(status) {
  const statusIcon = document.createElement("span");
  statusIcon.setAttribute("title", status);
  statusIcon.setAttribute("class", `fa fa-circle status ${status.toLowerCase()}`);
  return statusIcon;
}

function refreshList() {
  listContainer.innerHTML = "";
  createList();
}

function onDelete(evt) {
  const id = evt.target.parentElement.id;

  sendRequest({ method: 'delete', path: `/service/${id}` })
    .then((res) => {
      if (res.ok) refreshList();
    })
}

function onSave(evt) {
  evt.preventDefault();
  const name = document.querySelector('#service-name').value;
  const url = document.querySelector('#service-url').value;
  const body = JSON.stringify({ url, name });

  sendRequest({ method: 'post', path: '/service', body })
    .then((res) => {
      if (res.ok) { location.reload() }
      else res.text().then(setError);
    })
}

function sendRequest({ method, path, body }) {
  const fetchOptions = {
    method,
    headers: {
      'Accept': 'application/json, text/plain, */*',
      'Content-Type': 'application/json'
    },
    body,
  };
  return fetch(path, fetchOptions);
}

function setError(text) {
  document.getElementById('error').innerHTML = text;
}

function main() {
  const POLLING_INTERVAL_MS = 1000 * 5;
  createList()
  setInterval(() => refreshList(), POLLING_INTERVAL_MS);
  
  const saveButton = document.querySelector('#post-service');
  saveButton.onclick = onSave;
}

main();
