import React, { useEffect, useRef, useState } from 'react';
import { Loader2 } from 'lucide-react';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
const PLACEHOLDER_CLIENT_ID = 'your-google-client-id.apps.googleusercontent.com';

const isGoogleConfigured =
  Boolean(GOOGLE_CLIENT_ID) && GOOGLE_CLIENT_ID.trim() !== '' && GOOGLE_CLIENT_ID !== PLACEHOLDER_CLIENT_ID;

/**
 * Renders Google Identity Services "Continue with Google" button.
 * Sends the Google {@code idToken} (credential) to {@link onSuccess}.
 */
export default function GoogleSignInButton({ onSuccess, onError, disabled = false, label = 'Tiếp tục với Google' }) {
  const buttonContainerRef = useRef(null);
  const [gsiReady, setGsiReady] = useState(false);
  const [loading, setLoading] = useState(false);
  const [configNotice, setConfigNotice] = useState('');

  useEffect(() => {
    if (!isGoogleConfigured) return;

    let cancelled = false;
    let attempts = 0;

    const tryInitGoogle = () => {
      if (cancelled) return;
      if (window.google?.accounts?.id && buttonContainerRef.current) {
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: async (response) => {
            if (!response?.credential) {
              onError?.('Không nhận được Google ID Token.');
              return;
            }
            try {
              setLoading(true);
              await onSuccess(response.credential);
            } catch (err) {
              onError?.(err?.message || 'Đăng nhập Google thất bại.');
            } finally {
              if (!cancelled) setLoading(false);
            }
          },
        });

        buttonContainerRef.current.innerHTML = '';
        window.google.accounts.id.renderButton(buttonContainerRef.current, {
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'rectangular',
          width: buttonContainerRef.current.offsetWidth || 380,
          locale: 'vi',
        });
        setGsiReady(true);
        return;
      }

      if (attempts < 20) {
        attempts += 1;
        setTimeout(tryInitGoogle, 250);
      }
    };

    tryInitGoogle();
    return () => {
      cancelled = true;
    };
  }, [onSuccess, onError]);

  const handleFallbackClick = () => {
    if (!isGoogleConfigured) {
      setConfigNotice(
        'Chưa cấu hình VITE_GOOGLE_CLIENT_ID trong frontend-web/.env. Hãy điền Google Client ID để kích hoạt Google Sign-In.'
      );
      return;
    }
    if (!window.google?.accounts?.id) {
      onError?.('Không tải được Google Sign-In SDK (https://accounts.google.com/gsi/client). Hãy kiểm tra kết nối mạng hoặc tắt trình chặn quảng cáo.');
      return;
    }
    window.google.accounts.id.prompt();
  };

  return (
    <div className="w-full">
      {isGoogleConfigured ? (
        <div className="relative">
          <div
            ref={buttonContainerRef}
            className={`w-full min-h-[44px] flex justify-center ${disabled || loading ? 'opacity-60 pointer-events-none' : ''}`}
          />
          {!gsiReady && (
            <button
              type="button"
              disabled={disabled || loading}
              onClick={handleFallbackClick}
              className="w-full flex items-center justify-center gap-3 px-4 py-3 border-2 border-gray-200 rounded-xl font-semibold text-quizlet-textDark hover:bg-gray-50 transition"
            >
              <GoogleIcon />
              <span>{label}</span>
            </button>
          )}
          {loading && (
            <div className="mt-2 flex items-center justify-center gap-2 text-sm text-quizlet-blue font-medium">
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Đang xác thực tài khoản Google...</span>
            </div>
          )}
        </div>
      ) : (
        <>
          <button
            type="button"
            disabled={disabled}
            onClick={handleFallbackClick}
            className="w-full flex items-center justify-center gap-3 px-4 py-3 border-2 border-gray-200 rounded-xl font-semibold text-quizlet-textDark hover:bg-gray-50 hover:border-gray-300 transition"
          >
            <GoogleIcon />
            <span>{label}</span>
          </button>
          {configNotice && (
            <p className="mt-2 text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
              {configNotice}
            </p>
          )}
        </>
      )}
    </div>
  );
}

function GoogleIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"
      />
      <path
        fill="#34A853"
        d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24Z"
      />
      <path
        fill="#FBBC05"
        d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14Z"
      />
      <path
        fill="#EA4335"
        d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96Z"
      />
    </svg>
  );
}
