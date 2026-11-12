package br.com.senac.recursos.exception;

/**
 * 403 por regra de posse, decidida no serviço porque depende do dado (ex.: Solicitante mexendo na reserva de outro;
 * Responsável aprovando a própria reserva, RN-06). O 403 por perfil é do Spring Security (matriz de autorização).
 */
public class AcessoNegadoException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public String tipo() {
        return "acesso-negado";
    }
}
