import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Loader2, Mail, Lock, User, AtSign, GraduationCap, BookOpen } from 'lucide-react';
import AuthSplitLayout from '../components/auth/AuthSplitLayout';
import GoogleSignInButton from '../components/auth/GoogleSignInButton';
import authApi from '../api/authApi';
import { useAuthStore } from '../store/useAuthStore';

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
      setErrorMessage(firstFieldError || err?.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthSplitLayout mode="register">
      {errorMessage && (
        <div className="mb-5 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 font-medium">
          {errorMessage}
        </div>
      )}

      <GoogleSignInButton
        onSuccess={handleGoogleSignUp}
        onError={handleGoogleError}
        disabled={submitting}
        label="Đăng ký nhanh với Google"
      />

      <div className="my-5 flex items-center gap-4">
        <div className="h-px flex-1 bg-gray-200" />
        <span className="text-xs font-semibold uppercase tracking-wider text-quizlet-textMuted">
          hoặc email
        </span>
        <div className="h-px flex-1 bg-gray-200" />
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Quizlet-style Role Selector */}
        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-2">
            Bạn là
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setRole('USER')}
              className={`flex items-center gap-2.5 p-3 rounded-xl border-2 text-left transition ${
                role === 'USER'
                  ? 'border-quizlet-blue bg-blue-50/70 text-quizlet-blue'
                  : 'border-gray-200 bg-white text-quizlet-textMuted hover:border-gray-300'
              }`}
            >
              <BookOpen className="w-5 h-5 shrink-0" />
              <div>
                <div className="text-sm font-bold text-quizlet-textDark">Học viên</div>
                <div className="text-[11px] text-quizlet-textMuted">Học thẻ & chơi game</div>
              </div>
            </button>

            <button
              type="button"
              onClick={() => setRole('TEACHER')}
              className={`flex items-center gap-2.5 p-3 rounded-xl border-2 text-left transition ${
                role === 'TEACHER'
                  ? 'border-quizlet-blue bg-blue-50/70 text-quizlet-blue'
                  : 'border-gray-200 bg-white text-quizlet-textMuted hover:border-gray-300'
              }`}
            >
              <GraduationCap className="w-5 h-5 shrink-0" />
              <div>
                <div className="text-sm font-bold text-quizlet-textDark">Giáo viên</div>
                <div className="text-[11px] text-quizlet-textMuted">Tạo lớp & giao bài</div>
              </div>
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
              Họ và tên
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                required
                maxLength={100}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Văn A"
                className="w-full pl-10 pr-3 py-2.5 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
              Tên đăng nhập
            </label>
            <div className="relative">
              <AtSign className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
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
                className="w-full pl-10 pr-3 py-2.5 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
              />
            </div>
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
            Email
          </label>
          <div className="relative">
            <Mail className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@example.com"
              className="w-full pl-10 pr-4 py-2.5 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-quizlet-textMuted mb-1.5">
            Mật khẩu
          </label>
          <div className="relative">
            <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type={showPassword ? 'text' : 'password'}
              required
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Tối thiểu 6 ký tự"
              className="w-full pl-10 pr-11 py-2.5 bg-quizlet-bg border-2 border-transparent focus:border-quizlet-blue focus:bg-white rounded-xl text-sm font-medium text-quizlet-textDark outline-none transition"
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
          <span>Tạo tài khoản</span>
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-quizlet-textMuted">
        Đã có tài khoản?{' '}
        <Link to="/login" className="font-bold text-quizlet-blue hover:underline">
          Đăng nhập
        </Link>
      </p>
    </AuthSplitLayout>
  );
}
