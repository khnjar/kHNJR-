export const BenefitAgent = {
  name: 'Net Benefit (Γ)',
  calculate: (state: any, params: any) => {
    // Γ = Σ (Benefit_i - Risk_i)
    // تبسيط: قياس كفاءة الحالة بالنسبة للأهداف المستهدفة
    const efficiency = 1 - (Math.abs(state.heartRate - params.targetHeartRate) / 200);
    return Math.max(0, efficiency * 100);
  }
};
