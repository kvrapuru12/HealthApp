package com.healthapp.api;

/**
 * Machine-readable {@code code} values for JSON error bodies.
 * <p>
 * <b>401 — missing / invalid / expired credentials</b> (use {@link #UNAUTHORIZED},
 * {@link #INVALID_REFRESH_TOKEN}, {@link #INVALID_ID_TOKEN} as appropriate).
 * <p>
 * <b>403 — authorization</b> (authenticated but not allowed): {@link #FORBIDDEN}.
 * <p>
 * <b>403 — narrow “session ended” signal</b> (optional): {@link #SESSION_EXPIRED}.
 * The client may treat this like logout / re-login without treating every 403 as auth failure.
 * Emit only from deliberately documented controller paths; security filter defaults use
 * {@link #FORBIDDEN}.
 */
public final class ApiErrorCode {

    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String INVALID_ID_TOKEN = "INVALID_ID_TOKEN";

    public static final String FORBIDDEN = "FORBIDDEN";
    /** Reserved for documented narrow cases; pair with HTTP 403 only when specified in API docs. */
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";

    private ApiErrorCode() {}
}
