package br.com.fiap.zelo.dto;

public record LoginRequestDto(
        String email,
        String username,
        String senha,
        String password
) {
    public String obterIdentificador() {
        if (email != null && !email.isBlank()) return email.trim();
        if (username != null && !username.isBlank()) return username.trim();
        return "";
    }

    public String obterSenha() {
        if (senha != null && !senha.isBlank()) return senha;
        if (password != null && !password.isBlank()) return password;
        return "";
    }
}
