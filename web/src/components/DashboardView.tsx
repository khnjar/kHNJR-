import React, { useEffect } from 'react';
import { usePhysiologySimulation } from '../hooks/usePhysiologySimulation';
import { useBluetoothMonitoring } from '../hooks/useBluetoothMonitoring';
import { useMedicalReport } from '../hooks/useMedicalReport';
import { useTextToSpeech } from '../hooks/useTextToSpeech';
import { SimulationLab } from './SimulationLab';

const styles = {
  container: {
    padding: '20px',
    maxWidth: '1200px',
    margin: '0 auto',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
    gap: '20px',
  },
  module: {
    padding: '20px',
    border: '1px solid #ccc',
    borderRadius: '8px',
    backgroundColor: '#fff',
  }
};

export const DashboardView: React.FC = () => {
  const { state, history, params, updateParam } = usePhysiologySimulation({
    targetHeartRate: 70,
    targetBloodPressure: 120,
    targetOxygenSaturation: 98,
  });
  
  const { isConnected, data, connectBluetooth } = useBluetoothMonitoring();
  const { generateReport } = useMedicalReport();
  const { speak } = useTextToSpeech();
  const [loading, setLoading] = React.useState(false);

  const handleGenerateReport = async () => {
    setLoading(true);
    const report = await generateReport(state);
    speak(report);
    setLoading(false);
  };

  // Automatically update simulation if Bluetooth data is received
  useEffect(() => {
    if (data.heartRate) {
      updateParam('targetHeartRate', data.heartRate);
    }
  }, [data.heartRate, updateParam]);

  return (
    <div style={styles.container}>
      <h1>Medical Encyclopedia Dashboard</h1>
      
      <div style={{ marginBottom: '20px' }}>
        <button onClick={connectBluetooth} disabled={isConnected}>
          {isConnected ? 'Device Connected' : 'Connect Heart Rate Monitor'}
        </button>
        <button onClick={handleGenerateReport} disabled={loading} style={{ marginLeft: '10px' }}>
          {loading ? 'Generating...' : 'Generate & Speak Report'}
        </button>
      </div>

      <div style={styles.grid}>
        <section style={styles.module}>
          <h2>Clinical Calculators</h2>
          <p>Module to house BMI, eGFR, CHA₂DS₂-VASc calculators.</p>
        </section>
        
        <section style={styles.module}>
          <h2>Simulation & Heatmap Lab</h2>
          <SimulationLab state={state} history={history} />
          
          <div style={{ marginTop: '20px' }}>
            <h3>Control Panel</h3>
            <div>
              <label>Target Heart Rate: {params.targetHeartRate}</label>
              <input 
                type="range" min="40" max="150" value={params.targetHeartRate} 
                onChange={(e) => updateParam('targetHeartRate', Number(e.target.value))} 
              />
            </div>
            <div>
              <label>Target Blood Pressure: {params.targetBloodPressure}</label>
              <input 
                type="range" min="80" max="200" value={params.targetBloodPressure} 
                onChange={(e) => updateParam('targetBloodPressure', Number(e.target.value))} 
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};
