export const useMedicalReport = () => {
  const generateReport = async (state: { heartRate: number; bloodPressure: number; oxygenSaturation: number }) => {
    const prompt = `Act as a senior physician. Analyze these patient metrics and provide a professional, concise medical summary (under 50 words): HR=${state.heartRate.toFixed(0)} bpm, BP=${state.bloodPressure.toFixed(0)} mmHg, SpO2=${state.oxygenSaturation.toFixed(0)}%.`;
    
    const API_KEY = import.meta.env.VITE_GEMINI_API_KEY;
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${API_KEY}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }]
        })
      }
    );
    
    const data = await response.json();
    return data.candidates[0].content.parts[0].text;
  };

  return { generateReport };
};
