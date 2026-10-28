export const RiskAgent = {
  name: 'Risk Index (Ω)',
  calculate: (state: any, params: any) => {
    // Ω = التنبؤ باحتمالية الفشل بناءً على الانحراف عن المستهدفات
    const heartRisk = Math.abs(state.heartRate - params.targetHeartRate) / 100;
    const bpRisk = Math.abs(state.bloodPressure - params.targetBloodPressure) / 100;
    return Math.min(100, (heartRisk + bpRisk) * 50);
  }
};
