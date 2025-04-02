const express = require('express');
const router = express.Router();
const eventsController = require('../controllers/eventsController');

router.get('/', eventsController.getEvents);
router.get('/firms', eventsController.getFirmsData);

module.exports = router;
