require('dotenv').config();

const app = require('./app');
const PORT = 80;

require('./jobs/fetchDisasterDataJob');

app.listen(PORT, () => {
  console.log(`Tempest Backend: Server running on port ${PORT}`);
});
