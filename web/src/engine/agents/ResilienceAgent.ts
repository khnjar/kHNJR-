export const ResilienceAgent = {
  name: 'Resilience (ℛ)',
  calculate: (history: any[]) => {
    // ℛ = قدرة النظام على العودة للمستويات الطبيعية
    if (history.length < 10) return 100;
    const recent = history.slice(-10);
    const trend = recent.reduce((sum, val, i) => i > 0 ? sum + (val.heartRate - recent[i-1].heartRate) : sum, 0);
    // إذا كان الميل باتجاه المستقر، المرونة عالية
    return Math.max(0, 100 - Math.abs(trend) * 10);
  }
};
