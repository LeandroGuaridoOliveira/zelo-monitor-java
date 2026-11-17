package br.com.fiap.zelo.controller;

import br.com.fiap.zelo.dto.CasaRequestDto;
import br.com.fiap.zelo.dto.RegraConfiguracaoDto;
import br.com.fiap.zelo.model.CasaMonitorada;
import br.com.fiap.zelo.model.RegraConfiguracao;
import br.com.fiap.zelo.service.CasaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/casas")
@RequiredArgsConstructor
@Tag(name = "Casas Monitoradas", description = "Endpoints de gerenciamento de casas e regras de monitoramento")
public class CasaController {

    private final CasaService casaService;

    @GetMapping
    @Operation(summary = "Lista casas monitoradas de forma paginada")
    public ResponseEntity<Page<CasaMonitorada>> listarCasas(
            @PageableDefault(page = 0, size = 10, sort = "nomeIdoso") Pageable pageable) {
        return ResponseEntity.ok(casaService.listarCasas(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma casa monitorada por ID")
    public ResponseEntity<CasaMonitorada> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(casaService.buscarPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova casa monitorada e cria suas regras padrão")
    public ResponseEntity<CasaMonitorada> criarCasa(@Valid @RequestBody CasaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(casaService.criarCasa(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de uma casa monitorada")
    public ResponseEntity<CasaMonitorada> atualizarCasa(
            @PathVariable UUID id,
            @Valid @RequestBody CasaRequestDto dto) {
        return ResponseEntity.ok(casaService.atualizarCasa(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma casa monitorada e suas configurações associadas")
    public ResponseEntity<Void> removerCasa(@PathVariable UUID id) {
        casaService.removerCasa(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/regras")
    @Operation(summary = "Consulta as regras de monitoramento configuradas para a casa")
    public ResponseEntity<RegraConfiguracao> buscarRegras(@PathVariable UUID id) {
        return ResponseEntity.ok(casaService.buscarRegras(id));
    }

    @PutMapping("/{id}/regras")
    @Operation(summary = "Atualiza os limites e tolerâncias das regras de monitoramento da casa")
    public ResponseEntity<RegraConfiguracao> atualizarRegras(
            @PathVariable UUID id,
            @Valid @RequestBody RegraConfiguracaoDto dto) {
        return ResponseEntity.ok(casaService.atualizarRegras(id, dto));
    }
}
