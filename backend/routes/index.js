const express = require('express');
const router = express.Router();
const indexController = require('../controllers/indexController');

router.get('/', indexController.home);
router.post('/test_cron', indexController.testCron);

module.exports = router;
