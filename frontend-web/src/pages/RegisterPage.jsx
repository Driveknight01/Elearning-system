import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Loader2 } from 'lucide-react';
import AuthSplitLayout from '../components/auth/AuthSplitLayout';
import GoogleSignInButton from '../components/auth/GoogleSignInButton';
import authApi from '../api/authApi';
import { useAuthStore } from '../store/useAuthStore';

const AUTH_INPUT_CLASS =
  'w-full h-14 px-4 bg-[#F3F4F8] hover:bg-[#ECEEF5] border-2 border-transparent hover:border-[#4255FF]/60 focus:bg-white focus:hover:bg-white focus:border-[#4255FF] rounded-xl text-[16px] font-semibold tracking-tight text-[#282E3E] placeholder:text-[#646F90] placeholder:font-semibold caret-[#4255FF] outline-none transition-all duration-150';

export default function RegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, setAuth } = useAuthStore();

  const redirectTo = location.state?.from?.pathname || '/';

  const [role, setRole] = useState('USER');
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true });
    }
  }, [isAuthenticated, navigate, redirectTo]);

  const handleAuthSuccess = useCallback(
    (apiRes) => {
      const payload = apiRes?.data;
      if (!payload?.accessToken) {
        setErrorMessage('Phản hồi đăng ký không hợp lệ.');
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

  const handleGoogleSignUp = useCallback(
    async (idToken) => {
      setErrorMessage('');
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
    setErrorMessage(msg || 'Không thể đăng ký bằng Google.');
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!acceptedTerms) {
      setErrorMessage('Vui lòng đồng ý với Điều khoản dịch vụ và Chính sách bảo mật.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await authApi.register({
        username: username.trim(),
        email: email.trim(),
        password,
        fullName: fullName.trim(),
        role,
        platform: 'WEB',
        deviceInfo: navigator.userAgent.slice(0, 120),
      });
      handleAuthSuccess(res);
    } catch (err) {
      const firstFieldError = err?.errors ? Object.values(err.errors)[0] : null;
      setErrorMessage(
        firstFieldError || err?.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthSplitLayout mode="register">
      {errorMessage && (
        <div className="mb-5 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm font-semibold text-red-700">
          {errorMessage}
        </div>
      )}

      <GoogleSignInButton
        onSuccess={handleGoogleSignUp}
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

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Compact Segmented Role Toggle */}
        <div>
          <label className="block text-[14px] leading-none font-bold tracking-tight text-[#586380] mb-2">
            Bạn là
          </label>
          <div className="grid grid-cols-2 p-1 rounded-full bg-[#F3F4F8]">
            <button
              type="button"
              onClick={() => setRole('USER')}
              className={`h-11 rounded-full text-[15px] font-bold tracking-tight transition ${
                role === 'USER'
                  ? 'bg-white text-[#282E3E] shadow-sm'
                  : 'text-[#586380] hover:text-[#282E3E]'
              }`}
            >
              Học viên
            </button>
            <button
              type="button"
              onClick={() => setRole('TEACHER')}
              className={`h-11 rounded-full text-[15px] font-bold tracking-tight transition ${
                role === 'TEACHER'
                  ? 'bg-white text-[#282E3E] shadow-sm'
                  : 'text-[#586380] hover:text-[#282E3E]'
              }`}
            >
              Giáo viên
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-[14px] font-bold tracking-tight text-[#586380] mb-2">
              Họ và tên
            </label>
            <input
              type="text"
              required
              maxLength={100}
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nguyễn Văn A"
              className={AUTH_INPUT_CLASS}
            />
          </div>

          <div>
            <label className="block text-[14px] font-bold tracking-tight text-[#586380] mb-2">
              Tên người dùng
            </label>
            <input
              type="text"
              required
              minLength={3}
              maxLength={50}
              pattern="^[a-zA-Z0-9._-]+$"
              title="Chỉ chứa chữ cái, số, dấu chấm, gạch dưới hoặc gạch ngang"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="minh_learner"
              className={AUTH_INPUT_CLASS}
            />
          </div>
        </div>

        <div>
          <label className="block text-[14px] font-bold tracking-tight text-[#586380] mb-2">
            Email
          </label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="user@email.com"
            className={AUTH_INPUT_CLASS}
          />
        </div>

        <div>
          <label className="block text-[14px] font-bold tracking-tight text-[#586380] mb-2">
            Mật khẩu
          </label>
          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              required
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
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

        <label className="flex items-center gap-2.5 cursor-pointer select-none pt-1">
          <input
            type="checkbox"
            checked={acceptedTerms}
            onChange={(e) => setAcceptedTerms(e.target.checked)}
            className="w-4 h-4 rounded border-[#D9DDE8] text-[#4255FF] focus:ring-[#4255FF]"
          />
          <span className="text-[13px] font-medium text-[#586380]">
            Tôi đồng ý với{' '}
            <span className="font-bold text-[#282E3E]">Điều khoản dịch vụ</span> và{' '}
            <span className="font-bold text-[#282E3E]">Chính sách bảo mật</span> của SmartFlash
          </span>
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="w-full h-14 rounded-full bg-[#4255FF] hover:bg-[#3545E0] disabled:opacity-60 text-white text-[16px] font-bold tracking-tight flex items-center justify-center gap-2 transition-colors"
        >
          {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
          <span>Đăng ký</span>
        </button>

        <Link
          to="/login"
          className="w-full h-14 rounded-full bg-[#F3F4F8] hover:bg-[#E2E5EE] text-[#586380] hover:text-[#282E3E] text-[15px] font-bold tracking-tight flex items-center justify-center transition-colors"
        >
          Đã có tài khoản? Đăng nhập
        </Link>
      </form>
    </AuthSplitLayout>
  );
}
