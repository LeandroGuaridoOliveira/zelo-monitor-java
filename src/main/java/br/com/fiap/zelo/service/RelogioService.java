package br.com.fiap.zelo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RelogioService {

    @Value("${app.zelo.modo-simulacao:true}")
    private boolean modoSimulacao;

    private final Map<UUID, Instant> relogios = new ConcurrentHashMap<>();

    public void atualizarRelogio(UUID casaId, Instant agora) {
        if (modoSimulacao && casaId != null && agora != null) {
            relogios.put(casaId, agora);
        }
    }

    public Instant obterAgora(UUID casaId) {
        if (modoSimulacao && casaId != null && relogios.containsKey(casaId)) {
            return relogios.get(casaId);
        }
        return Instant.now();
    }

    public void resetar(UUID casaId) {
        if (casaId != null) {
            relogios.remove(casaId);
        } else {
            relogios.clear();
        }
    }

    public boolean isModoSimulacao() {
        return modoSimulacao;
    }

    public void setModoSimulacao(boolean modoSimulacao) {
        this.modoSimulacao = modoSimulacao;
    }
}
