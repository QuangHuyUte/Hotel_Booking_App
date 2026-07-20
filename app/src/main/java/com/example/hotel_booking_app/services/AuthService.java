package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.User;
import com.example.hotel_booking_app.data.remote.SupabaseAuthClient;
import com.example.hotel_booking_app.data.remote.SupabaseAuthSession;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class AuthService {
    private final SupabaseClient supabaseClient;
    private final SupabaseAuthClient authClient;

    public AuthService() {
        supabaseClient = SupabaseClient.getInstance();
        authClient = new SupabaseAuthClient();
    }

    public void login(String email, String password, SupabaseCallback<User> callback) {
        authClient.signInWithPassword(email.trim(), password, new SupabaseCallback<SupabaseAuthSession>() {
            @Override
            public void onSuccess(SupabaseAuthSession session) {
                supabaseClient.setAccessToken(session.getAccessToken());
                loadPublicUserAfterAuth(email, session, callback);
            }

            @Override
            public void onError(String message) {
                loginWithPublicUsersFallback(email, password, callback);
            }
        });
    }

    public void sendPasswordResetEmail(String email, SupabaseCallback<Boolean> callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onError("Please enter your email.");
            return;
        }
        authClient.recoverPassword(email.trim(), callback);
    }

    public void loginWithOAuthSession(String fullName, String email, String authUserId, String accessToken, String refreshToken, SupabaseCallback<User> callback) {
        if (email == null || email.trim().isEmpty() || authUserId == null || authUserId.trim().isEmpty()) {
            callback.onError("Google login did not return enough account information.");
            return;
        }
        SupabaseAuthSession session = new SupabaseAuthSession(authUserId, email.trim(), accessToken, refreshToken);
        supabaseClient.setAccessToken(accessToken);
        String resolvedName = fullName == null || fullName.trim().isEmpty() ? email.trim() : fullName.trim();
        ensurePublicUser(resolvedName, email.trim(), "GOOGLE_OAUTH", "", AppConstants.ROLE_CUSTOMER, session, callback);
    }

    public void loginOrCreateGoogleAccount(String fullName, String email, SupabaseCallback<User> callback) {
        if (email == null || !email.trim().toLowerCase().endsWith("@gmail.com")) {
            callback.onError("Please choose a valid Gmail account.");
            return;
        }
        String cleanEmail = email.trim();
        String cleanName = fullName == null || fullName.trim().isEmpty()
                ? cleanEmail.substring(0, cleanEmail.indexOf("@"))
                : fullName.trim();
        Map<String, String> filters = new HashMap<>();
        filters.put("email", cleanEmail);
        supabaseClient.getList(AppConstants.TABLE_USERS, "*", 1, null, filters, User[].class, new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (!users.isEmpty()) {
                    User existing = users.get(0);
                    attachDemoGoogleSession(existing, cleanEmail);
                    createOtpThenReturn(existing, callback);
                    return;
                }
                createGooglePublicUser(cleanName, cleanEmail, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void createGooglePublicUser(String fullName, String email, SupabaseCallback<User> callback) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(BCrypt.hashpw("GOOGLE_ACCOUNT_LINKED", BCrypt.gensalt()));
        user.setPhone("");
        user.setNationalId("GOOGLE-" + Math.abs(email.hashCode()));
        user.setAddress("Google account");
        user.setNationality("Vietnamese");
        user.setRole(AppConstants.ROLE_CUSTOMER);
        attachDemoGoogleSession(user, email);
        supabaseClient.insert(AppConstants.TABLE_USERS, user, User[].class, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User createdUser) {
                attachDemoGoogleSession(createdUser, email);
                callback.onSuccess(createdUser);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void createOtpThenReturn(User user, SupabaseCallback<User> callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", user.getEmail());
        payload.put("otp", String.format("%06d", new Random().nextInt(1_000_000)));
        payload.put("userId", user.getId());
        payload.put("expiresAt", LocalDateTime.now().plusMinutes(10).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        supabaseClient.insertNoReturn(AppConstants.TABLE_OTPS, payload, new SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(String message) {
                callback.onSuccess(user);
            }
        });
    }

    private void attachDemoGoogleSession(User user, String email) {
        user.setAuthUserId("google-demo-" + UUID.nameUUIDFromBytes(email.getBytes()).toString());
        user.setAuthAccessToken("google-demo-token");
        user.setAuthRefreshToken("google-demo-refresh");
    }

    private void loginWithPublicUsersFallback(String email, String password, SupabaseCallback<User> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("email", email.trim());
        supabaseClient.getList(AppConstants.TABLE_USERS, "*", 1, null, filters, User[].class, new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users.isEmpty()) {
                    callback.onError("Email chưa tồn tại.");
                    return;
                }

                User user = users.get(0);
                if (!isPasswordValid(password, user.getPassword())) {
                    callback.onError("Mật khẩu không đúng.");
                    return;
                }
                syncExistingPublicUserToSupabaseAuth(user, password, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void syncExistingPublicUserToSupabaseAuth(User publicUser, String password, SupabaseCallback<User> callback) {
        authClient.signUp(publicUser.getEmail(), password, new SupabaseCallback<SupabaseAuthSession>() {
            @Override
            public void onSuccess(SupabaseAuthSession session) {
                supabaseClient.setAccessToken(session.getAccessToken());
                attachAuthSession(publicUser, session);
                callback.onSuccess(publicUser);
            }

            @Override
            public void onError(String signUpError) {
                authClient.signInWithPassword(publicUser.getEmail(), password, new SupabaseCallback<SupabaseAuthSession>() {
                    @Override
                    public void onSuccess(SupabaseAuthSession session) {
                        supabaseClient.setAccessToken(session.getAccessToken());
                        attachAuthSession(publicUser, session);
                        callback.onSuccess(publicUser);
                    }

                    @Override
                    public void onError(String signInError) {
                        callback.onError("Tài khoản tồn tại trong public.users nhưng chưa đồng bộ được Supabase Auth. Hãy tạo lại tài khoản bằng màn đăng ký hoặc kiểm tra Supabase Auth cho email này.");
                    }
                });
            }
        });
    }

    public void register(String fullName, String email, String password, String phone, SupabaseCallback<User> callback) {
        register(fullName, email, password, phone, AppConstants.ROLE_CUSTOMER, callback);
    }

    public void register(String fullName, String email, String password, String phone, String role, SupabaseCallback<User> callback) {
        authClient.signUp(email.trim(), password, new SupabaseCallback<SupabaseAuthSession>() {
            @Override
            public void onSuccess(SupabaseAuthSession session) {
                supabaseClient.setAccessToken(session.getAccessToken());
                registerPublicUser(fullName, email, password, phone, role, session, callback);
            }

            @Override
            public void onError(String message) {
                trySignInAfterRegisterFailure(fullName, email, password, phone, role, message, callback);
            }
        });
    }

    private void trySignInAfterRegisterFailure(
            String fullName,
            String email,
            String password,
            String phone,
            String role,
            String originalError,
            SupabaseCallback<User> callback
    ) {
        authClient.signInWithPassword(email.trim(), password, new SupabaseCallback<SupabaseAuthSession>() {
            @Override
            public void onSuccess(SupabaseAuthSession session) {
                supabaseClient.setAccessToken(session.getAccessToken());
                ensurePublicUser(fullName, email, password, phone, role, session, callback);
            }

            @Override
            public void onError(String signInError) {
                callback.onError("Không tạo được Supabase Auth user: " + originalError);
            }
        });
    }

    private void registerPublicUser(String fullName, String email, String password, String phone, String role, SupabaseAuthSession session, SupabaseCallback<User> callback) {
        ensurePublicUser(fullName, email, password, phone, role, session, callback);
    }

    private void ensurePublicUser(String fullName, String email, String password, String phone, String role, SupabaseAuthSession session, SupabaseCallback<User> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("email", email.trim());
        supabaseClient.getList(AppConstants.TABLE_USERS, "*", 1, null, filters, User[].class, new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (!users.isEmpty()) {
                    User existingUser = users.get(0);
                    attachAuthSession(existingUser, session);
                    callback.onSuccess(existingUser);
                    return;
                }

                User user = new User();
                user.setFullName(fullName.trim());
                user.setEmail(email.trim());
                user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
                user.setPhone(phone.trim());
                user.setRole(role);
                user.setAuthUserId(session.getUserId());
                user.setAuthAccessToken(session.getAccessToken());
                user.setAuthRefreshToken(session.getRefreshToken());
                supabaseClient.insert(AppConstants.TABLE_USERS, user, User[].class, new SupabaseCallback<User>() {
                    @Override
                    public void onSuccess(User createdUser) {
                        attachAuthSession(createdUser, session);
                        callback.onSuccess(createdUser);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void loadPublicUserAfterAuth(String email, SupabaseAuthSession session, SupabaseCallback<User> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("email", email.trim());
        supabaseClient.getList(AppConstants.TABLE_USERS, "*", 1, null, filters, User[].class, new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                User user;
                if (users.isEmpty()) {
                    callback.onError("Đăng nhập Auth thành công nhưng chưa có profile trong public.users cho email này.");
                    return;
                } else {
                    user = users.get(0);
                }
                attachAuthSession(user, session);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String message) {
                callback.onError("Không tải được profile public.users: " + message);
            }
        });
    }

    private void attachAuthSession(User user, SupabaseAuthSession session) {
        user.setAuthUserId(session.getUserId());
        user.setAuthAccessToken(session.getAccessToken());
        user.setAuthRefreshToken(session.getRefreshToken());
    }

    public void getUserById(String userId, SupabaseCallback<User> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", userId);
        supabaseClient.getSingle(AppConstants.TABLE_USERS, filters, User[].class, callback);
    }

    public void getFirstUserByRole(String role, SupabaseCallback<User> callback) {
        Map<String, String> filters = new HashMap<>();
        filters.put("role", role);
        supabaseClient.getSingle(AppConstants.TABLE_USERS, filters, User[].class, callback);
    }

    public void getSupportUser(SupabaseCallback<User> callback) {
        getFirstUserByRole(AppConstants.ROLE_ADMIN, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onError(String adminError) {
                getFirstUserByRole(AppConstants.ROLE_HOST, callback);
            }
        });
    }

    public void updateProfile(User user, SupabaseCallback<User> callback) {
        String dateOfBirth = cleanOptional(user.getDateOfBirth());
        if (dateOfBirth != null) {
            try {
                LocalDate.parse(dateOfBirth);
            } catch (DateTimeParseException e) {
                callback.onError("Date of birth must use YYYY-MM-DD format, for example 2000-05-23.");
                return;
            }
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("_id", user.getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("fullName", cleanRequired(user.getFullName()));
        payload.put("phone", cleanOptional(user.getPhone()));
        payload.put("nationalId", cleanOptional(user.getNationalId()));
        payload.put("dateOfBirth", dateOfBirth);
        payload.put("address", cleanOptional(user.getAddress()));
        payload.put("gender", cleanOptional(user.getGender()));
        payload.put("nationality", cleanOptional(user.getNationality()));
        supabaseClient.update(AppConstants.TABLE_USERS, filters, payload, User[].class, callback);
    }

    public void updatePassword(String userId, String newPassword, SupabaseCallback<User> callback) {
        if (newPassword == null || newPassword.length() < 6) {
            callback.onError("Password must be at least 6 characters.");
            return;
        }
        Map<String, String> filters = new HashMap<>();
        filters.put("_id", userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("password", BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        supabaseClient.update(AppConstants.TABLE_USERS, filters, payload, User[].class, callback);
    }

    private boolean isPasswordValid(String password, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2")) {
            return BCrypt.checkpw(password, storedPassword);
        }
        return storedPassword.equals(password);
    }

    private String cleanOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String cleanRequired(String value) {
        return value == null ? "" : value.trim();
    }
}
