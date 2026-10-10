package com.proyecto.servicios.validation;

/**
 * Expresiones regulares compartidas entre los DTOs de request (body) y los
 * parametros de query (ej. ClienteController.buscar), para no duplicar el
 * mismo patron en dos lugares y que ambos queden siempre sincronizados.
 */
public final class PatronesValidacion {

    public static final String CURP =
            "^[A-Z][AEIOU][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HM]" +
                    "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QO|QR|SP|SL|SR|TC|TL|TS|VZ|YN|ZS|NE)" +
                    "[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d$";

    public static final String RFC = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$";

    public static final String NUMERO_CUENTA = "^\\d{10}$";

    private PatronesValidacion() {
    }
}
