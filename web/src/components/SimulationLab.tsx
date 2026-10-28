import React from 'react';
import { PhysiologicalState } from '../hooks/usePhysiologySimulation';
import { HeatmapNode } from './HeatmapNode';
import { PhysiologyChart } from './PhysiologyChart';

interface Props {
  state: PhysiologicalState;
  history: PhysiologicalState[];
}

export const SimulationLab: React.FC<Props> = ({ state, history }) => {
  return (
    <div>
      <div style={{ display: 'flex', flexWrap: 'wrap' }}>
        <HeatmapNode value={state.heartRate} label="Heart Rate" />
        <HeatmapNode value={state.bloodPressure} label="Blood Pressure" />
        <HeatmapNode value={state.oxygenSaturation} label="Oxygen Sat" />
      </div>
      <div style={{ marginTop: '10px', padding: '10px', backgroundColor: '#f0f0f0' }}>
        <strong>ICM Metrics:</strong><br />
        Entropy(Ψ): {state.metrics.entropy.toFixed(2)} | 
        Benefit(Γ): {state.metrics.benefit.toFixed(2)} | 
        Innovation(Π): {state.metrics.innovation.toFixed(2)}<br />
        Risk(Ω): {state.metrics.risk.toFixed(2)} | 
        Resilience(ℛ): {state.metrics.resilience.toFixed(2)}
      </div>
      <PhysiologyChart history={history} />
    </div>
  );
};
