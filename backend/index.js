const http = require('http');
const os = require('os');

// Retrieve Server's IP Address
function getServerIp() {
  const interfaces = os.networkInterfaces();
  for (const name in interfaces) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return 'IP not found';
}

// Retrieve Server Local Time in GMT Format
function getLocalTime() {
  const now = new Date();

  // Parse GMT Variables from Date
  const hours = now.getHours().toString().padStart(2, '0');
  const minutes = now.getMinutes().toString().padStart(2, '0');
  const seconds = now.getSeconds().toString().padStart(2, '0');
  const offset = -now.getTimezoneOffset();
  const offsetHours = Math.floor(offset / 60).toString().padStart(2, '0');
  const offsetMinutes = (offset % 60).toString().padStart(2, '0');

  return `${hours}:${minutes}:${seconds} GMT ${offsetHours}:${offsetMinutes}`;
}

// Server Endpoints
http.createServer(function (req, res) {
  
  // GET /server_ip
  if (req.url === '/server_ip') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ip: getServerIp() }));
  } 

  // GET /local_time
  else if (req.url === '/local_time') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ time: getLocalTime() }));
  } 

  // GET /name
  else if (req.url === '/name') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ 
        firstName: 'Martin', 
        lastName: 'Tang', 
        studentNumber: 37737640 
    }));
  }
  
  // Server Error Handling Endpoint
  else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Endpoint not found');
  }
  
}).listen(80, () => {
    console.log(getServerIp())
    console.log('Server running on port 80');
});