package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.ZonaHoraria;
import org.springframework.http.HttpStatus;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.entity.clientes.Cliente;
import com.proyecto.servicios.entity.clientes.Login;
import com.proyecto.servicios.repositorys.clientes.ClienteRepository;
import com.proyecto.servicios.repositorys.clientes.LoginRepository;
import com.proyecto.servicios.security.JwtService;
import com.proyecto.servicios.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    // Mismo mensaje para correo inexistente, contrasena incorrecta o cliente
    // dado de baja, para no revelar cual de los tres fallo.
    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Correo o contrasena incorrectos";

    private final ClienteRepository clienteRepository;
    private final LoginRepository loginRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${security.session.inactividad-minutos}")
    private long inactividadMinutos;

    public LoginServiceImpl(ClienteRepository clienteRepository,
                             LoginRepository loginRepository,
                             JwtService jwtService) {
        this.clienteRepository = clienteRepository;
        this.loginRepository = loginRepository;
        this.jwtService = jwtService;
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
    public String iniciarSesion(String correoElectronico, String passwordPlano) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(correoElectronico)
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES_INVALIDAS));

        Login login = loginRepository.findByClienteId(cliente.getId())
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES_INVALIDAS));

        Optional.of(login.getPasswordHash())
                .filter(hash -> passwordEncoder.matches(passwordPlano, hash))
                .orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES_INVALIDAS));

        String token = jwtService.generarToken(cliente.getId(), cliente.getCorreoElectronico());

        login.setJwtToken(token);
        login.setSesionActiva(true);
        login.setFechaUltimoAcceso(LocalDateTime.now(ZonaHoraria.MEXICO));
        loginRepository.save(login);

        log.info("Sesion iniciada. clienteId={}", cliente.getId());
        return token;
    }

    @Override
    public void cerrarSesion(Integer clienteId) {
        loginRepository.findByClienteId(clienteId).ifPresent(login -> {
            login.setSesionActiva(false);
            login.setJwtToken(null);
            loginRepository.save(login);
        });
        log.info("Sesion cerrada. clienteId={}", clienteId);
    }

    @Override
    @Scheduled(cron = "${security.session.check-cron:0 * * * * *}")
    public void cerrarSesionesInactivas() {
        LocalDateTime limite = LocalDateTime.now(ZonaHoraria.MEXICO).minusMinutes(inactividadMinutos);
        List<Login> sesionesInactivas = loginRepository.findBySesionActivaTrueAndFechaUltimoAccesoBefore(limite);

        sesionesInactivas.forEach(login -> {
            login.setSesionActiva(false);
            login.setJwtToken(null);
        });
        loginRepository.saveAll(sesionesInactivas);

        log.info("Revision de sesiones inactivas completada. sesiones cerradas={}", sesionesInactivas.size());
    }
}
