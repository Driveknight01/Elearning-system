import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Loader2, ArrowLeft, CheckCircle2 } from 'lucide-react';
import AuthSplitLayout from '../components/auth/AuthSplitLayout';
import GoogleSignInButton from '../components/auth/GoogleSignInButton';
import authApi from '../api/authApi';
import { useAuthStore } from '../store/useAuthStore';

const AUTH_INPUT_CLASS =
  'w-full h-14 px-4 bg-[#F3F4F8] hover:bg-[#ECEEF5] border-2 border-transparent hover:border-[#4255FF]/60 focus:bg-white focus:hover:bg-white focus:border-[#4255FF] rounded-xl text-[16px] font-semibold tracking-tight text-[#282E3E] placeholder:text-[#646F90] placeholder:font-semibold caret-[#4255FF] outline-none transition-all duration-150';

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
      setErrorMessage(err?.message || 'Email, tên người dùng hoặc mật khẩu không chính xác.');
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
      setSuccessMessage(res?.message || 'Mã OTP 6 số đã được gửi tới email của bạn.');
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
        <div className="mb-5 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm font-semibold text-red-700">
          {errorMessage}
        </div>
      )}

      {successMessage && (
        <div className="mb-5 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm font-semibold text-emerald-700 flex items-start gap-2">
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
            label="Đăng nhập với Google"
          />

          <div className="mt-7 mb-3 flex items-center gap-4">
            <div className="h-px flex-1 bg-[#EDEFF4]" />
            <span className="text-[13px] leading-none font-bold tracking-tight text-[#586380]">
              hoặc email
            </span>
            <div className="h-px flex-1 bg-[#EDEFF4]" />
          </div>

          <form onSubmit={handleLoginSubmit} className="space-y-4">
            <div>
              <label className="block text-[14px] leading-none font-bold tracking-tight text-[#586380] mb-2">
                Email
              </label>
              <input
                type="text"
                required
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder="Nhập địa chỉ email hoặc tên người dùng"
                className={AUTH_INPUT_CLASS}
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-[14px] font-bold tracking-tight text-[#586380]">
                  Mật khẩu
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setErrorMessage('');
                    setSuccessMessage('');
                    setViewMode('forgot');
                  }}
                  className="text-[14px] font-bold tracking-tight text-[#4255FF] hover:underline"
                >
                  Quên mật khẩu
                </button>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nhập mật khẩu của bạn"
                  className={`${AUTH_INPUT_CLASS} pr-12`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-[#586380] hover:text-[#282E3E] transition-colors"
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>

            <p className="text-center text-[13px] font-medium text-[#586380] leading-relaxed pt-1">
              Bằng cách nhấn Đăng nhập, bạn đồng ý với{' '}
              <span className="font-bold text-[#282E3E]">Điều khoản dịch vụ</span> và{' '}
              <span className="font-bold text-[#282E3E]">Chính sách bảo mật</span> của SmartFlash.
            </p>

            <button
              type="submit"
              disabled={submitting}
              className="w-full h-14 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-60 text-white text-[16px] font-bold tracking-tight flex items-center justify-center gap-2 transition-colors"
            >
              {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>Đăng nhập</span>
            </button>

            <Link
              to="/register"
              className="w-full h-14 rounded-full bg-[#F3F4F8] hover:bg-[#E2E5EE] text-[#586380] hover:text-[#282E3E] text-[15px] font-bold tracking-tight flex items-center justify-center transition-colors"
            >
              Chưa có tài khoản? Tạo tài khoản
            </Link>
          </form>
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
            className="inline-flex items-center gap-1.5 text-sm font-bold tracking-tight text-[#586380] hover:text-[#282E3E]"
          >
            <ArrowLeft className="w-4 h-4" /> Quay lại đăng nhập
          </button>

          <div>
            <h2 className="text-xl font-bold tracking-tight text-[#282E3E]">Khôi phục mật khẩu</h2>
            <p className="mt-1 text-sm font-medium text-[#586380]">
              Nhập email đã đăng ký để nhận mã OTP 6 chữ số (có hiệu lực 15 phút).
            </p>
          </div>

          <div>
            <label className="block text-[14px] font-bold tracking-tight text-[#586380] mb-2">
              Email tài khoản
            </label>
            <input
              type="email"
              required
              value={resetEmail}
              onChange={(e) => setResetEmail(e.target.value)}
              placeholder="name@example.com"
              className={AUTH_INPUT_CLASS}
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full h-14 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-60 text-white text-[16px] font-bold tracking-tight flex items-center justify-center gap-2 transition-colors"
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
            className="inline-flex items-center gap-1.5 text-sm font-bold tracking-tight text-[#586380] hover:text-[#282E3E]"
          >
            <ArrowLeft className="w-4 h-4" /> Gửi lại mã OTP
          </button>

          <div>
            <h2 className="text-xl font-bold tracking-tight text-[#282E3E]">Đặt mật khẩu mới</h2>
            <p className="mt-1 text-sm font-medium text-[#586380]">
              Nhập mã OTP 6 số vừa gửi tới{' '}
              <span className="font-bold text-[#282E3E]">{resetEmail}</span>.
            </p>
          </div>

          <div>
            <label className="block text-[14px] font-bold tracking-tight text-[#586380] mb-2">
              Mã OTP (6 chữ số)
            </label>
            <input
              type="text"
              required
              maxLength={6}
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              placeholder="123456"
              className={`${AUTH_INPUT_CLASS} font-bold tracking-widest`}
            />
          </div>

          <div>
            <label className="block text-[14px] font-bold tracking-tight text-[#586380] mb-2">
              Mật khẩu mới
            </label>
            <input
              type="password"
              required
              minLength={6}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Tối thiểu 6 ký tự"
              className={AUTH_INPUT_CLASS}
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full h-14 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-60 text-white text-[16px] font-bold tracking-tight flex items-center justify-center gap-2 transition-colors"
          >
            {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
            <span>Xác nhận đổi mật khẩu</span>
          </button>
        </form>
      )}
    </AuthSplitLayout>
  );
}
