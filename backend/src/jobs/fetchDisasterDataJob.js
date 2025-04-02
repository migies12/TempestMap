const cron = require('node-cron');
const { fetchDisasterData } = require('../services/weatherService');

// Schedule the job to run at the start of every hour
cron.schedule('0 * * * *', () => {
    fetchDisasterData()
        .then(() =>
            console.log('Disaster data fetched successfully via cron job')
        )
        .catch((error) =>
            console.error('Error fetching disaster data via cron job:', error)
        );
});
