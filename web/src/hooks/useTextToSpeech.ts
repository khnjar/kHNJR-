import { useCallback } from 'react';

export const useTextToSpeech = () => {
  const speak = useCallback((text: string) => {
    if (!('speechSynthesis' in window)) {
      console.error('TTS not supported');
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';
    window.speechSynthesis.speak(utterance);
  }, []);

  return { speak };
};
