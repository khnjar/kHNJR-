import { useState, useEffect, useCallback } from 'react';
import { EntropyAgent } from '../engine/agents/EntropyAgent';
import { BenefitAgent } from '../engine/agents/BenefitAgent';
import { InnovationAgent } from '../engine/agents/InnovationAgent';
import { RiskAgent } from '../engine/agents/RiskAgent';
import { ResilienceAgent } from '../engine/agents/ResilienceAgent';

export interface PhysiologicalState {
  heartRate: number;
  bloodPressure: number;
  oxygenSaturation: number;
  metrics: { entropy: number; benefit: number; innovation: number; risk: number; resilience: number };
}

export const usePhysiologySimulation = (initialParams: { [key: string]: number }) => {
  const [params, setParams] = useState(initialParams);
  const [state, setState] = useState<PhysiologicalState>({
    heartRate: 70,
    bloodPressure: 120,
    oxygenSaturation: 98,
    metrics: { entropy: 0, benefit: 0, innovation: 0, risk: 0, resilience: 100 }
  });
  const [history, setHistory] = useState<PhysiologicalState[]>([]);

  const derivatives = useCallback((currentState: PhysiologicalState, currentParams: { [key: string]: number }) => {
    return {
      heartRate: (currentParams.targetHeartRate - currentState.heartRate) * 0.1,
      bloodPressure: (currentParams.targetBloodPressure - currentState.bloodPressure) * 0.05,
      oxygenSaturation: (currentParams.targetOxygenSaturation - currentState.oxygenSaturation) * 0.01,
    };
  }, []);

  useEffect(() => {
    const interval = setInterval(() => {
      setState(prevState => {
        const d = derivatives(prevState, params);
        const newStateValue = {
          heartRate: prevState.heartRate + d.heartRate,
          bloodPressure: prevState.bloodPressure + d.bloodPressure,
          oxygenSaturation: prevState.oxygenSaturation + d.oxygenSaturation,
        };
        const newState = {
          ...newStateValue,
          metrics: {
            entropy: EntropyAgent.calculate(newStateValue, params),
            benefit: BenefitAgent.calculate(newStateValue, params),
            innovation: InnovationAgent.calculate(newStateValue, params),
            risk: RiskAgent.calculate(newStateValue, params),
            resilience: ResilienceAgent.calculate(history)
          }
        };
        setHistory(prev => [...prev.slice(-49), newState]); // Keep last 50 points
        return newState;
      });
    }, 100);
    return () => clearInterval(interval);
  }, [params, derivatives, history]);

  const updateParam = (key: string, value: number) => {
    setParams(prev => ({ ...prev, [key]: value }));
  };

  return { state, history, params, updateParam };
};
