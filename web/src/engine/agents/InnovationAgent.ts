export const InnovationAgent = {
  name: 'Innovation Index (Π)',
  calculate: (state: any, params: any) => {
    // Π = قياس مدى اختلاف الحالة الراهنة عن المستهدفات
    const deviation = Math.abs(state.heartRate - params.targetHeartRate) / 100;
    return Math.min(100, deviation * 60 + 15);
  }
};
