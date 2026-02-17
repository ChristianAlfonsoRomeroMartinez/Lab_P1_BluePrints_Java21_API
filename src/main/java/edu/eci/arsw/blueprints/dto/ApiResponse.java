package edu.eci.arsw.blueprints.dto;

//manejo de respuestas para el front

//record se usa para clases inmutables con pocos campos, ideal para DTOs simples
// Una clase inmutable no cambia despues de ser creada

public record ApiResponse<T>(int code, String message, T data) {

    //Uso factory para las respuestas previstas

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "Created", data);
    }

    public static <T> ApiResponse<T> accepted() {
        return new ApiResponse<>(202, "Accepted", null);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    public static <T> ApiResponse<T> internalError(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
}
