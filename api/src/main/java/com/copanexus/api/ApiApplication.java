package com.copanexus.api;

import com.copanexus.api.repository.FigurinhasRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import java.util.ArrayList;
import java.util.List;
import com.copanexus.api.model.Figurinhas;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApiApplication {
	@Bean
	CommandLineRunner popularFigurinhas(FigurinhasRepository repository) {
		return args -> {
			// Verifica se a tabela já está populada para não duplicar dados
			if (repository.count() == 0) {
				System.out.println(">>> Iniciando carga de 980 figurinhas no Neon...");

				List<Figurinhas> listaParaSalvar = new ArrayList<>();

				// Loop de 0 a 979
				for (int i = 0; i <= 979; i++) {
					Figurinhas f = new Figurinhas();
					f.setNumero(i);
					listaParaSalvar.add(f);
				}

				// Salva tudo de uma vez (Batch Save) - Mais rápido!
				repository.saveAll(listaParaSalvar);

				System.out.println(">>> Sucesso! 980 figurinhas prontas para colecionar.");
			} else {
				System.out.println(">>> Figurinhas detectadas no banco. Pulando carga.");
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
