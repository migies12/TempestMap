const express = require('express');
const router = express.Router();
const markersController = require('../controllers/markersController');

router.post('/', markersController.createUserMarker);
router.get('/', markersController.getUserMarkers);

module.exports = router;
