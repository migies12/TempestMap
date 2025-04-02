const express = require('express');
const router = express.Router();
const usersController = require('../controllers/usersController');

router.post('/', usersController.createUser);
router.get('/:user_id', usersController.getUser);
router.post('/locations', usersController.updateUserLocation);

module.exports = router;
