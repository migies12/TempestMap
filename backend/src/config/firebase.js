const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');

const firebaseApp = initializeApp({
  credential: applicationDefault(),
  projectId: 'tempestmap-f0234'
});

module.exports = { firebaseApp, getMessaging };