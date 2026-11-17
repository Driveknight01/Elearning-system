import { useCallback, useEffect, useRef, useState } from 'react';

const LANG_LOCALE_MAP = {
  en: 'en-US',
  vi: 'vi-VN',
  ja: 'ja-JP',
  ko: 'ko-KR',
  zh: 'zh-CN',
  fr: 'fr-FR',
  de: 'de-DE',
  es: 'es-ES',
  it: 'it-IT',
  ru: 'ru-RU',
  th: 'th-TH',
};

const VIETNAMESE_DIACRITIC_REGEX =
  /[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i;

const PREFERRED_FEMALE_VOICE_KEYWORDS = [
  'aria online',
  'jenny online',
  'hoaimy',
  'google us english',
  'google uk english female',
  'samantha',
  'zira',
  'karen',
  'victoria',
  'natural',
  'neural',
  'female',
];

export function resolveSpeechLocale(langCode, text = '') {
  const cleanText = typeof text === 'string' ? text.trim() : '';

  // Smart override: if text has Vietnamese diacritics, always use vi-VN;
  // if set metadata says 'vi' but text is pure ASCII English (e.g. English-English sets), use en-US.
  if (cleanText) {
    if (VIETNAMESE_DIACRITIC_REGEX.test(cleanText)) {
      return 'vi-VN';
    }
    if (
      (!langCode || langCode.toLowerCase().startsWith('vi')) &&
      /^[a-zA-Z0-9\s.,'"!?;:()\-/]+$/.test(cleanText)
    ) {
      return 'en-US';
    }
  }

  if (!langCode || typeof langCode !== 'string') return 'en-US';
  const trimmed = langCode.trim();
  if (!trimmed) return 'en-US';
  if (trimmed.includes('-')) return trimmed;
  const lower = trimmed.toLowerCase();
  return LANG_LOCALE_MAP[lower] || `${lower}-${lower.toUpperCase()}`;
}

function scoreVoice(voice, targetLocale) {
  const vLang = (voice.lang || '').toLowerCase();
  const vName = (voice.name || '').toLowerCase();
  const prefix = targetLocale.split('-')[0].toLowerCase();

  let score = 0;
  if (vLang === targetLocale.toLowerCase()) score += 100;
  else if (vLang.startsWith(prefix)) score += 60;
  else return -1;

  for (let i = 0; i < PREFERRED_FEMALE_VOICE_KEYWORDS.length; i += 1) {
    if (vName.includes(PREFERRED_FEMALE_VOICE_KEYWORDS[i])) {
      score += 50 - i * 3;
      break;
    }
  }

  if (vName.includes('online') || vName.includes('natural')) score += 25;
  if (vName.includes('google')) score += 15;
  return score;
}

/**
 * React hook wrapping Web Speech API (`window.speechSynthesis`) with smart language detection
 * and natural female voice prioritization.
 */
export function useTextToSpeech() {
  const isSupported =
    typeof window !== 'undefined' &&
    'speechSynthesis' in window &&
    typeof window.SpeechSynthesisUtterance !== 'undefined';

  const [speakingKey, setSpeakingKey] = useState(null);
  const [voices, setVoices] = useState([]);
  const activeUtteranceRef = useRef(null);

  useEffect(() => {
    if (!isSupported) return undefined;

    const loadVoices = () => {
      const available = window.speechSynthesis.getVoices() || [];
      setVoices(available);
    };

    loadVoices();
    window.speechSynthesis.addEventListener?.('voiceschanged', loadVoices);

    return () => {
      window.speechSynthesis.removeEventListener?.('voiceschanged', loadVoices);
      window.speechSynthesis.cancel();
    };
  }, [isSupported]);

  const stop = useCallback(() => {
    if (!isSupported) return;
    window.speechSynthesis.cancel();
    activeUtteranceRef.current = null;
    setSpeakingKey(null);
  }, [isSupported]);

  const speak = useCallback(
    (text, langCode = 'en', key = null, { rate = 0.96 } = {}) => {
      if (!isSupported) return false;
      const cleanText = typeof text === 'string' ? text.trim() : '';
      if (!cleanText) return false;

      if (key && speakingKey === key && window.speechSynthesis.speaking) {
        stop();
        return false;
      }

      window.speechSynthesis.cancel();

      const locale = resolveSpeechLocale(langCode, cleanText);
      const utterance = new window.SpeechSynthesisUtterance(cleanText);
      utterance.lang = locale;
      utterance.rate = rate;

      const availableVoices =
        voices.length > 0 ? voices : window.speechSynthesis.getVoices() || [];
      let bestVoice = null;
      let bestScore = -1;
      for (const v of availableVoices) {
        const s = scoreVoice(v, locale);
        if (s > bestScore) {
          bestScore = s;
          bestVoice = v;
        }
      }
      if (bestVoice) {
        utterance.voice = bestVoice;
      }

      utterance.onstart = () => {
        setSpeakingKey(key || 'active');
      };
      utterance.onend = () => {
        if (activeUtteranceRef.current === utterance) {
          activeUtteranceRef.current = null;
          setSpeakingKey(null);
        }
      };
      utterance.onerror = () => {
        if (activeUtteranceRef.current === utterance) {
          activeUtteranceRef.current = null;
          setSpeakingKey(null);
        }
      };

      activeUtteranceRef.current = utterance;
      setSpeakingKey(key || 'active');
      window.speechSynthesis.speak(utterance);
      return true;
    },
    [isSupported, speakingKey, stop, voices]
  );

  return {
    isSupported,
    isSpeaking: Boolean(speakingKey),
    speakingKey,
    speak,
    stop,
  };
}

export default useTextToSpeech;
