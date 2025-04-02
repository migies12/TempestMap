const dangerLevelCalc = (lat1, lon1, lat2, lon2, disasterType) => {
    const baseDangerLevels = {
      WF: 100,  // Wildfire
      EQ: 80,   // Earthquake
      FL: 75,   // Flood
      TS: 70,   // Tropical storm
      HU: 95,   // Hurricane
      TO: 90,   // Tornado
      BZ: 65,   // Blizzard
      VO: 85,   // Volcano
      LS: 70    // Landslide
    };
  
    const R = 6371000; // Radius of Earth in meters
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(lat1 * (Math.PI / 180)) *
        Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c;
    const danger = baseDangerLevels[disasterType];
    const distanceFactor = 1.0 - Math.min(1.0, distance / 500000.0);
    return Math.round(danger * distanceFactor);
  };
  
  module.exports = dangerLevelCalc;
  