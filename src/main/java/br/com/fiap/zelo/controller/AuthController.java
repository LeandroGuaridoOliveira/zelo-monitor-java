package br.com.fiap.zelo.controller;

import br.com.fiap.zelo.dto.AuthResponseDto;
import br.com.fiap.zelo.dto.LoginRequestDto;
import br.com.fiap.zelo.dto.SignupRequestDto;
import br.com.fiap.zelo.security.JwtUtil;
import br.com.fiap.zelo.security.UserConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/auth", "/auth"})
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints de login e registro com emissão de token JWT")
public class AuthController {

    private final UserConfig userConfig;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    @Operation(summary = "Realiza login e emite token JWT com as claims do usuário")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto dto) {
        String identificador = dto.obterIdentificador();
        String senha = dto.obterSenha();

        UserConfig.UsuarioInfo usuario = userConfig.buscarPorIdentificador(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos."));

        if (!usuario.senha().equals(senha)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }

        String token = jwtUtil.generateToken(
                usuario.username() != null ? usuario.username() : usuario.email(),
                usuario.nome(),
                usuario.papel(),
                usuario.idosoId(),
                usuario.casaId()
        );

        AuthResponseDto response = new AuthResponseDto(
                token,
                token,
                "Bearer",
                jwtUtil.getExpirationMs(),
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                usuario.papel(),
                usuario.idosoId(),
                usuario.casaId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @Operation(summary = "Cadastra um novo usuário e retorna token JWT")
    public ResponseEntity<AuthResponseDto> signup(@RequestBody SignupRequestDto dto) {
        String identificador = dto.obterIdentificador();
        String senha = dto.obterSenha();
        String papel = dto.papel() != null && !dto.papel().isBlank() ? dto.papel().toUpperCase().trim() : "FAMILIAR";
        String nome = dto.nome() != null && !dto.nome().isBlank() ? dto.nome() : identificador;

        UUID id = UUID.randomUUID();
        UserConfig.UsuarioInfo novoUsuario = new UserConfig.UsuarioInfo(
                id,
                nome,
                dto.email(),
                dto.username(),
                senha,
                papel,
                dto.idosoId(),
                dto.casaId()
        );

        userConfig.cadastrar(novoUsuario);

        String token = jwtUtil.generateToken(
                novoUsuario.username() != null ? novoUsuario.username() : novoUsuario.email(),
                novoUsuario.nome(),
                novoUsuario.papel(),
                novoUsuario.idosoId(),
                novoUsuario.casaId()
        );

        AuthResponseDto response = new AuthResponseDto(
                token,
                token,
                "Bearer",
                jwtUtil.getExpirationMs(),
                novoUsuario.id(),
                novoUsuario.nome(),
                novoUsuario.email(),
                novoUsuario.papel(),
                novoUsuario.idosoId(),
                novoUsuario.casaId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
