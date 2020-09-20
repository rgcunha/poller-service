const listContainer = document.querySelector('#service-list');
const servicesRequest = new Request('/service');
fetch(servicesRequest)
  .then(response => response.json())
  .then((serviceList) => {
    serviceList.forEach(service => {
      const li = document.createElement("li");
      li.setAttribute("id", service.id);

      const deleteBtn = document.createElement("input");
      deleteBtn.setAttribute("value", "Delete");
      deleteBtn.setAttribute("type", "button");
      deleteBtn.onclick = onDelete;

      const statusIcon = document.createElement("span");
      statusIcon.setAttribute("class", "fa fa-check status");

      li.appendChild(document.createTextNode(service.name + ': ' + service.status));
      li.appendChild(statusIcon);
      li.appendChild(deleteBtn);
      listContainer.appendChild(li);
    });
  });

const saveButton = document.querySelector('#post-service');
saveButton.onclick = onSave;

function onDelete(evt) {
  const id = evt.target.parentElement.id;

  sendRequest({ method: 'delete', path: `/service/${id}` })
    .then((res) => {
      if (res.ok) location.reload()
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