package br.com.fiap.zelo.security;

import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class UserConfig {

    public record UsuarioInfo(
            UUID id,
            String nome,
            String email,
            String username,
            String senha,
            String papel,
            UUID idosoId,
            UUID casaId
    ) {}

    private final Map<String, UsuarioInfo> usuarios = new ConcurrentHashMap<>();

    public UserConfig() {
        // Dona Maria (Idosa)
        cadastrar(new UsuarioInfo(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Dona Maria",
                "maria@zelo.com",
                "maria",
                "123456",
                "IDOSO",
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000100")
        ));

        // Ana (Filha / Familiar -> Cuidador)
        cadastrar(new UsuarioInfo(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "Ana",
                "ana@zelo.com",
                "ana",
                "123456",
                "FAMILIAR",
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000100")
        ));

        // Seu José (Vizinho / Guardião -> Cuidador)
        cadastrar(new UsuarioInfo(
                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                "Seu José",
                "jose@zelo.com",
                "jose",
                "123456",
                "GUARDIAO",
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000100")
        ));

        // Administrador
        cadastrar(new UsuarioInfo(
                UUID.fromString("00000000-0000-0000-0000-000000000004"),
                "Administrador",
                "admin@zelo.com",
                "admin",
                "123456",
                "ADMIN",
                null,
                null
        ));

        // Serviço
        cadastrar(new UsuarioInfo(
                UUID.fromString("00000000-0000-0000-0000-000000000005"),
                "Serviço Monitoramento",
                "servico@zelo.com",
                "servico",
                "123456",
                "SERVICO",
                null,
                null
        ));
    }

    public void cadastrar(UsuarioInfo usuario) {
        if (usuario.email() != null) {
            usuarios.put(usuario.email().toLowerCase().trim(), usuario);
        }
        if (usuario.username() != null) {
            usuarios.put(usuario.username().toLowerCase().trim(), usuario);
        }
    }

    public Optional<UsuarioInfo> buscarPorIdentificador(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(usuarios.get(id.toLowerCase().trim()));
    }
}
