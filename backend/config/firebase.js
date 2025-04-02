const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');

const firebaseApp = initializeApp({
  credential: applicationDefault()
});

module.exports = { firebaseApp, getMessaging };