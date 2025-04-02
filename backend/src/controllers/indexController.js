const { fetchDisasterData } = require('../services/disasterService');

exports.home = (req, res) => {
    res.status(200).json({ message: 'Success' });
};

exports.testCron = async (req, res) => {
    try {
        await fetchDisasterData();
        res.status(200).json({
            message: 'Disaster data fetched successfully!',
        });
    } catch (error) {
        res.status(500).json({ error: 'Error fetching disaster data' });
    }
};
