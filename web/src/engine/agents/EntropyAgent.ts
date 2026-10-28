export interface EquationAgent {
  name: string;
  calculate: (state: any, params: any) => number;
}

export const EntropyAgent: EquationAgent = {
  name: 'Entropy (Ψ)',
  calculate: (state, params) => {
    // Ψ = Σ |(Mi - Mi_opt) / Mi_range|
    // تبسيط: قياس الانحراف عن الحالة المستقرة
    const hRateDeviation = Math.abs(state.heartRate - params.targetHeartRate) / 100;
    const bpDeviation = Math.abs(state.bloodPressure - params.targetBloodPressure) / 50;
    return hRateDeviation + bpDeviation;
  }
};
