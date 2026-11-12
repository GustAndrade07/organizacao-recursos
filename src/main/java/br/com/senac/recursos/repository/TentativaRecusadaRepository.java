package br.com.senac.recursos.repository;

import br.com.senac.recursos.model.TentativaRecusada;
import org.springframework.data.repository.Repository;

/** Só inclusão (D2): a tentativa recusada é evidência do relatório e não se altera nem se apaga. */
public interface TentativaRecusadaRepository extends Repository<TentativaRecusada, Long> {

    TentativaRecusada save(TentativaRecusada tentativa);
}
