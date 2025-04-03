// const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');

// const firebaseApp = initializeApp({
//   credential: applicationDefault(),
//   projectId: 'tempestmap-f0234'
// });

var admin = require("firebase-admin");

var serviceAccount = require("../tempestmap-f0234-firebase-adminsdk-fbsvc-72247d1ba6.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

module.exports = { admin, getMessaging };