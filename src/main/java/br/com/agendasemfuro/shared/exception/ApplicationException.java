package br.com.agendasemfuro.shared.exception;

/** An application-level failure that the HTTP boundary translates to a response. */
public final class ApplicationException extends RuntimeException {
    public enum Code {
        NOT_FOUND,
        INVALID_REQUEST,
        CONFLICT
    }

    private final Code code;

    private ApplicationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public static ApplicationException notFound(String message) {
        return new ApplicationException(Code.NOT_FOUND, message);
    }

    public static ApplicationException invalidRequest(String message) {
        return new ApplicationException(Code.INVALID_REQUEST, message);
    }

    public static ApplicationException conflict(String message) {
        return new ApplicationException(Code.CONFLICT, message);
    }

    public Code getCode() {
        return code;
    }
}
