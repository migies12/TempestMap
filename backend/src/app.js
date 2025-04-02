const express = require('express');
const app = express();

app.use(express.json());

const indexRoutes = require('./routes/index');
const eventRoutes = require('./routes/events');
const markersRoutes = require('./routes/markers');
const commentsRoutes = require('./routes/comments');
const usersRoutes = require('./routes/users');

app.use('/', indexRoutes);
app.use('/event', eventRoutes);
app.use('/user_marker', markersRoutes);
app.use('/comment', commentsRoutes);
app.use('/user', usersRoutes);

module.exports = app;
