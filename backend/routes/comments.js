const express = require('express');
const router = express.Router();
const commentsController = require('../controllers/commentsController');

router.post('/:id', commentsController.addComment);
router.get('/:event_id', commentsController.getComments);
router.delete('/:event_id', commentsController.deleteComment);

module.exports = router;
