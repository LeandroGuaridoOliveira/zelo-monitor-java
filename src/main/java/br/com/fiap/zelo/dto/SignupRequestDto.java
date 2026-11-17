package br.com.fiap.zelo.dto;

import java.util.UUID;

public record SignupRequestDto(
        String email,
        String username,
        String senha,
        String password,
        String nome,
        String papel,
        UUID idosoId,
        UUID casaId
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
