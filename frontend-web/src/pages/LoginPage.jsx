import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Loader2, Mail, Lock, KeyRound, ArrowLeft, CheckCircle2 } from 'lucide-react';
import AuthSplitLayout from '../components/auth/AuthSplitLayout';
import GoogleSignInButton from '../components/auth/GoogleSignInButton';
import authApi from '../api/authApi';
import { useAuthStore } from '../store/useAuthStore';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, setAuth } = useAuthStore();

  const redirectTo = location.state?.from?.pathname || '/';

  // Login state
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Forgot-password OTP state ('login' | 'forgot' | 'reset')
  const [viewMode, setViewMode] = useState('login');
  const [resetEmail, setResetEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [newPassword, setNewPassword] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true });
    }
  }, [isAuthenticated, navigate, redirectTo]);

  const handleAuthSuccess = useCallback(
    (apiRes) => {
      const payload = apiRes?.data;
      if (!payload?.accessToken) {
        setErrorMessage('Phản hồi đăng nhập không hợp lệ.');
        return;
      }
      setAuth({
        accessToken: payload.accessToken,
        refreshToken: payload.refreshToken,
        user: payload.user,
      });
      navigate(redirectTo, { replace: true });
    },
    [navigate, redirectTo, setAuth]
  );

  const handleGoogleLogin = useCallback(
    async (idToken) => {
      setErrorMessage('');
      setSuccessMessage('');
      const res = await authApi.googleLogin({
        idToken,
        platform: 'WEB',
        deviceInfo: navigator.userAgent.slice(0, 120),
      });
      handleAuthSuccess(res);
    },
    [handleAuthSuccess]
  );

  const handleGoogleError = useCallback((msg) => {
    setErrorMessage(msg || 'Không thể đăng nhập bằng Google.');
  }, []);

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setSubmitting(true);

    try {
      const res = await authApi.login({
        identifier: identifier.trim(),
        password,
        platform: 'WEB',
        deviceInfo: navigator.userAgent.slice(0, 120),
      });
      handleAuthSuccess(res);
    } catch (err) {
      setErrorMessage(err?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setSubmitting(true);

    try {
      const res = await authApi.forgotPassword(resetEmail.trim());
      setSuccessMessage(
        res?.message || 'Mã OTP 6 số đã được gửi tới email của bạn.'
      );
      setViewMode('reset');
    } catch (err) {
      setErrorMessage(err?.message || 'Không thể gửi mã OTP. Vui lòng kiểm tra lại email.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setSubmitting(true);

    try {
      await authApi.resetPassword({
        email: resetEmail.trim(),
        otpCode: otpCode.trim(),
        newPassword,
      });
      setViewMode('login');
      setOtpCode('');
      setNewPassword('');
      setSuccessMessage('Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.');
    } catch (err) {
      const fieldError = err?.errors ? Object.values(err.errors)[0] : null;
      setErrorMessage(fieldError || err?.message || 'Mã OTP không chính xác hoặc đã hết hạn.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthSplitLayout mode="login">
      {errorMessage && (
        <div className="mb-5 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 font-medium">
          {errorMessage}
        </div>
      )}

      {successMessage && (
        <div className="mb-5 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-700 font-medium flex items-start gap-2">
          <CheckCircle2 className="w-4 h-4 mt-0.5 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {viewMode === 'login' && (
        <>
          <GoogleSignInButton
            onSuccess={handleGoogleLogin}
            onError={handleGoogleError}
            disabled={submitting}
          />

          <div className="my-6 flex items-center gap-4">
            <div className="h-px flex-1 bg-gray-200" />
            <span className="text-xs font-semibold uppercase tracking-wider text-quizlet-textMuted">
              hoặc email
            </span>
            <div className="h-px flex-1 bg-gray-200" />
          </div>

          <form onSubmit={handleLoginSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
                Email hoặc Tên đăng nhập
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder="name@example.com hoặc username"
                  className="w-full pl-10 pr-4 py-3 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted">
                  Mật khẩu
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setErrorMessage('');
                    setSuccessMessage('');
                    setViewMode('forgot');
                  }}
                  className="text-xs font-semibold text-quizlet-blue hover:underline"
                >
                  Quên mật khẩu?
                </button>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nhập mật khẩu"
                  className="w-full pl-10 pr-11 py-3 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-quizlet-textDark"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-quizlet-blue hover:bg-[#3546E0] disabled:opacity-60 text-white font-bold py-3.5 rounded-xl shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 transition"
            >
              {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>Đăng nhập</span>
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-quizlet-textMuted">
            Chưa có tài khoản SmartFlash?{' '}
            <Link to="/register" className="font-bold text-quizlet-blue hover:underline">
              Đăng ký miễn phí
            </Link>
          </p>
        </>
      )}

      {viewMode === 'forgot' && (
        <form onSubmit={handleSendOtp} className="space-y-4">
          <button
            type="button"
            onClick={() => {
              setErrorMessage('');
              setViewMode('login');
            }}
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-quizlet-textMuted hover:text-quizlet-textDark"
          >
            <ArrowLeft className="w-4 h-4" /> Quay lại đăng nhập
          </button>

          <div>
            <h2 className="text-xl font-extrabold text-quizlet-textDark">Khôi phục mật khẩu</h2>
            <p className="mt-1 text-sm text-quizlet-textMuted">
              Nhập email đã đăng ký để nhận mã OTP 6 chữ số (có hiệu lực 15 phút).
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
              Email tài khoản
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={resetEmail}
                onChange={(e) => setResetEmail(e.target.value)}
                placeholder="name@example.com"
                className="w-full pl-10 pr-4 py-3 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-quizlet-blue hover:bg-[#3546E0] disabled:opacity-60 text-white font-bold py-3.5 rounded-xl flex items-center justify-center gap-2 transition"
          >
            {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
            <span>Gửi mã OTP</span>
          </button>
        </form>
      )}

      {viewMode === 'reset' && (
        <form onSubmit={handleResetPassword} className="space-y-4">
          <button
            type="button"
            onClick={() => {
              setErrorMessage('');
              setViewMode('forgot');
            }}
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-quizlet-textMuted hover:text-quizlet-textDark"
          >
            <ArrowLeft className="w-4 h-4" /> Gửi lại mã OTP
          </button>

          <div>
            <h2 className="text-xl font-extrabold text-quizlet-textDark">Đặt mật khẩu mới</h2>
            <p className="mt-1 text-sm text-quizlet-textMuted">
              Nhập mã OTP 6 số vừa gửi tới <span className="font-semibold text-quizlet-textDark">{resetEmail}</span>.
            </p>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
              Mã OTP (6 chữ số)
            </label>
            <div className="relative">
              <KeyRound className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                maxLength={6}
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value)}
                placeholder="123456"
                className="w-full pl-10 pr-4 py-3 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-bold tracking-widest text-quizlet-textDark outline-none transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
              Mật khẩu mới
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                minLength={6}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Tối thiểu 6 ký tự"
                className="w-full pl-10 pr-4 py-3 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-quizlet-blue hover:bg-[#3546E0] disabled:opacity-60 text-white font-bold py-3.5 rounded-xl flex items-center justify-center gap-2 transition"
          >
            {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
            <span>Xác nhận đổi mật khẩu</span>
          </button>
        </form>
      )}
    </AuthSplitLayout>
  );
}
