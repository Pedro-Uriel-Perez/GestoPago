package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.ZonaHoraria;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Login;
import com.proyecto.servicios.entity.clientes.Sesion;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.clientes.SesionResponse;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.LoginRepository;
import com.proyecto.servicios.repositorys.clientes.SesionRepository;
import com.proyecto.servicios.security.JwtService;
import com.proyecto.servicios.service.LoginService;
import com.proyecto.servicios.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    // Mismo mensaje para correo inexistente, contrasena incorrecta o cliente
    // dado de baja, para no revelar cual de los tres fallo.
    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Correo o contrasena incorrectos";

    private final ClienteRepository clienteRepository;
    private final LoginRepository loginRepository;
    private final SesionRepository sesionRepository;
    private final JwtService jwtService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${security.session.inactividad-minutos}")
    private long inactividadMinutos;

    public LoginServiceImpl(ClienteRepository clienteRepository,
                             LoginRepository loginRepository,
                             SesionRepository sesionRepository,
                             JwtService jwtService,
                             MailService mailService) {
        this.clienteRepository = clienteRepository;
        this.loginRepository = loginRepository;
        this.sesionRepository = sesionRepository;
        this.jwtService = jwtService;
        this.mailService = mailService;
    }

    @Override
    public void registrarCredenciales(Cliente cliente, String passwordPlano) {
        Login login = new Login();
        login.setCliente(cliente);
        login.setCorreoLogin(cliente.getCorreoElectronico());
        login.setNombreCifrado(cliente.getNombre());
        login.setPasswordHash(passwordEncoder.encode(passwordPlano));
        login.setSesionActiva(false);
        loginRepository.save(login);
    }

    @Override
    public String iniciarSesion(String correoElectronico, String passwordPlano, String dispositivo) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(correoElectronico)
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES_INVALIDAS));

        Login login = loginRepository.findByClienteId(cliente.getId())
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES_INVALIDAS));

        Optional.of(login.getPasswordHash())
                .filter(hash -> passwordEncoder.matches(passwordPlano, hash))
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES_INVALIDAS));

        String dispositivoSesion = Optional.ofNullable(dispositivo).orElse(Sesion.DISPOSITIVO_WEB);
        List<Sesion> sesionesPrevias = sesionRepository.findByClienteIdAndActivaTrueOrderByFechaInicioDesc(cliente.getId());

        // Una sesion por dispositivo: si ya habia una en este mismo tipo de
        // dispositivo se reemplaza; la del otro dispositivo se queda abierta.
        List<Sesion> reemplazadas = sesionesPrevias.stream()
                .filter(sesion -> dispositivoSesion.equals(sesion.getDispositivo()))
                .toList();
        reemplazadas.forEach(Sesion::cerrar);
        sesionRepository.saveAll(reemplazadas);

        Sesion sesion = new Sesion();
        sesion.setClienteId(cliente.getId());
        sesion.setIdentificador(UUID.randomUUID().toString());
        sesion.setDispositivo(dispositivoSesion);
        sesion.setJwtToken(jwtService.generarToken(
                cliente.getId(), cliente.getCorreoElectronico(), sesion.getIdentificador()));
        sesionRepository.save(sesion);

        if (!sesionesPrevias.isEmpty()) {
            avisarNuevoInicioDeSesion(cliente, dispositivoSesion);
        }

        log.info("Sesion iniciada. clienteId={}, dispositivo={}", cliente.getId(), dispositivoSesion);
        return sesion.getJwtToken();
    }

    /**
     * La alerta es informativa: si el correo falla no se le niega el inicio
     * de sesion al cliente (ademas cada app muestra el aviso por su cuenta
     * consultando las sesiones activas).
     */
    private void avisarNuevoInicioDeSesion(Cliente cliente, String dispositivo) {
        try {
            mailService.enviarAlertaInicioSesion(
                    cliente.getCorreoElectronico(), dispositivo, LocalDateTime.now(ZonaHoraria.MEXICO));
        } catch (MailException e) {
            log.warn("No se pudo enviar la alerta de inicio de sesion. clienteId={}", cliente.getId(), e);
        }
    }

    @Override
    public void cerrarSesion(Integer clienteId, String sesionId) {
        sesionRepository.findByIdentificador(sesionId)
                .filter(sesion -> sesion.getClienteId().equals(clienteId))
                .ifPresent(sesion -> {
                    sesion.cerrar();
                    sesionRepository.save(sesion);
                });
        log.info("Sesion cerrada. clienteId={}", clienteId);
    }

    @Override
    public void cerrarTodasLasSesiones(Integer clienteId) {
        List<Sesion> sesiones = sesionRepository.findByClienteIdAndActivaTrueOrderByFechaInicioDesc(clienteId);
        sesiones.forEach(Sesion::cerrar);
        sesionRepository.saveAll(sesiones);
        log.info("Sesiones cerradas. clienteId={}, total={}", clienteId, sesiones.size());
    }

    @Override
    public List<SesionResponse> obtenerSesionesActivas(Integer clienteId, String sesionActualId) {
        return sesionRepository.findByClienteIdAndActivaTrueOrderByFechaInicioDesc(clienteId).stream()
                .map(sesion -> new SesionResponse(
                        sesion.getDispositivo(),
                        sesion.getFechaInicio(),
                        sesion.getFechaUltimoAcceso(),
                        sesion.getIdentificador().equals(sesionActualId)))
                .toList();
    }

    @Override
    @Scheduled(cron = "${security.session.check-cron:0 * * * * *}")
    public void cerrarSesionesInactivas() {
        LocalDateTime limite = LocalDateTime.now(ZonaHoraria.MEXICO).minusMinutes(inactividadMinutos);
        List<Sesion> sesionesInactivas = sesionRepository.findByActivaTrueAndFechaUltimoAccesoBefore(limite);

        sesionesInactivas.forEach(Sesion::cerrar);
        sesionRepository.saveAll(sesionesInactivas);

        log.info("Revision de sesiones inactivas completada. sesiones cerradas={}", sesionesInactivas.size());
    }
}
