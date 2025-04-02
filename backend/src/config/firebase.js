//const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');

/*const firebaseApp = initializeApp({
  credential: applicationDefault(),
  projectId: 'tempestmap-f0234'
});*/

var admin = require("firebase-admin");

var serviceAccount = require("/home/ec2-user/tempestmap-f0234-firebase-adminsdk-fbsvc-cb8db3f6e9.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

module.exports = { admin, getMessaging };