package br.com.senac.recursos.exception;

/** 400: dado de entrada que passa na validação de formato mas fere uma regra (ex.: RN-01, término antes do início). */
public class RequisicaoInvalidaException extends NegocioException {

    private static final long serialVersionUID = 1L;

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    @Override
    public String tipo() {
        return "requisicao-invalida";
    }
}
